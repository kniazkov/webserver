/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests failures and timeout conditions handled by {@link Worker}.
 * <p>
 * These tests cover errors occurring at the worker level: malformed requests,
 * handler failures, handler execution timeouts and clients that stop sending
 * data. Lower-level parser and response errors are tested separately.
 */
final class WorkerErrorTest extends WorkerBaseTest {

    /**
     * Tests processing a malformed HTTP request.
     */
    @Test
    void malformedRequest() throws Exception {
        final Options options = new Options.Builder().build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "THIS-IS-A-DIAGNOSTIC-MARKER\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 400")
            );
            assertTrue(response.text().contains("Bad Request"));
            assertFalse(
                response.text().contains(
                    "THIS-IS-A-DIAGNOSTIC-MARKER"
                )
            );

            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());
        }
    }

    /**
     * Tests a handler that reports a server error.
     */
    @Test
    void handlerThrowsServerException() throws Exception {
        final Handler handler = (request, environment) -> {
            throw new ServerException("Handler failed");
        };

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 500")
            );
            assertFalse(response.text().contains("Handler failed"));
        }
    }

    /**
     * Tests a handler that deliberately reports a client-visible HTTP error.
     */
    @Test
    void handlerThrowsHttpError() throws Exception {
        final Handler handler = (request, environment) -> {
            throw new ServerException(
                HttpStatus.CONFLICT,
                "Resource already exists"
            );
        };

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 409")
            );
            assertTrue(
                response.text().contains("Resource already exists")
            );
        }
    }

    /**
     * Tests an unexpected runtime exception thrown by a handler.
     */
    @Test
    void handlerThrowsRuntimeException() throws Exception {
        final Handler handler = (request, environment) -> {
            throw new IllegalStateException("Something exploded");
        };

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 500")
            );
            assertFalse(
                response.text().contains("Something exploded")
            );
        }
    }

    /**
     * Tests termination of a handler that exceeds its execution timeout.
     */
    @Test
    void handlerTimeout() throws Exception {
        final Handler handler = (request, environment) -> {
            try {
                Thread.sleep(Duration.ofSeconds(10));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }

            return environment
                .getResponseFactory()
                .fromText("Too late")
                .build();
        };

        final Options options = new Options.Builder()
            .setHandler(handler)
            .setHandlerTimeout(Duration.ofMillis(100))
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 503")
            );

            assertFalse(
                response.text().contains("Too late")
            );
        }
    }

    /**
     * Tests a client that connects but sends no request data.
     */
    @Test
    void idleClientTimeout() throws Exception {
        final Options options = new Options.Builder()
            .setReadTimeout(Duration.ofMillis(100))
            .build();

        try (Connection connection = connect(options)) {
            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());

            assertEquals(
                -1,
                connection.socket().getInputStream().read()
            );
        }
    }

    /**
     * Tests a client that starts a request but stops sending data before the
     * request headers are complete.
     */
    @Test
    void incompleteRequestTimeout() throws Exception {
        final Options options = new Options.Builder()
            .setReadTimeout(Duration.ofMillis(100))
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET / HTTP/1.1\r\n"
                    + "Host: local"
            );

            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());

            assertEquals(
                -1,
                connection.socket().getInputStream().read()
            );
        }
    }

    /**
     * Tests a client that sends complete headers but stops in the middle of
     * the declared request body.
     */
    @Test
    void incompleteBodyTimeout() throws Exception {
        final Options options = new Options.Builder()
            .setReadTimeout(Duration.ofMillis(100))
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: 100\r\n"
                    + "\r\n"
                    + "partial"
            );

            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());

            assertEquals(
                -1,
                connection.socket().getInputStream().read()
            );
        }
    }

    /**
     * Tests rejection of request headers exceeding the configured limit.
     */
    @Test
    void headersTooLarge() throws Exception {
        final String request =
            "POST / HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Length: 100\r\n"
                + "Connection: close\r\n"
                + "\r\n"
                + "x".repeat(100);

        final Options options = new Options.Builder()
            .setMaxHeaderSize(64)
            .setMaxFileSize(64)
            .setMaxRequestSize(80)
            .build();

        try (Connection connection = connect(options)) {
            send(connection.socket(), request);

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 431")
            );

            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());
        }
    }

    /**
     * Tests rejection of a complete request exceeding the configured limit.
     */
    @Test
    void requestTooLarge() throws Exception {
        final String body = "x".repeat(300);
        final String request =
            "POST / HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Length: " + body.length() + "\r\n"
                + "Connection: close\r\n"
                + "\r\n"
                + body;

        final Options options = new Options.Builder()
            .setMaxHeaderSize(256)
            .setMaxFileSize(64)
            .setMaxRequestSize(256)
            .build();

        try (Connection connection = connect(options)) {
            send(connection.socket(), request);

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 413")
            );
            assertTrue(
                response.text().contains("Payload Too Large")
            );
            assertFalse(
                response.text().contains(
                    "Maximum HTTP request size exceeded"
                )
            );
        }
    }

    /**
     * Tests protocol-specific errors for unsupported request features.
     */
    @Test
    void unsupportedProtocolFeatures() throws Exception {
        final Options options = new Options.Builder().build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "DELETE / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 501")
            );
            assertTrue(response.text().contains("Not Implemented"));
            assertFalse(
                response.text().contains("Unsupported HTTP method")
            );
        }

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET / HTTP/2.0\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 505")
            );
            assertTrue(
                response.text().contains(
                    "HTTP Version Not Supported"
                )
            );
            assertFalse(
                response.text().contains("Unsupported HTTP version")
            );
        }

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Transfer-Encoding: chunked\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 501")
            );
            assertFalse(
                response.text().contains(
                    "Transfer-Encoding is not supported"
                )
            );
        }
    }

}
