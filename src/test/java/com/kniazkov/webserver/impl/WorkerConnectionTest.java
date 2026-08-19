/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests connection management and persistent HTTP connections handled by
 * {@link Worker}.
 */
final class WorkerConnectionTest extends WorkerBaseTest {

    /**
     * Tests processing several requests through one persistent connection.
     */
    @Test
    void keepAlive() throws Exception {
        final AtomicInteger counter = new AtomicInteger();

        final Handler handler = (request, environment) ->
            environment
                .getResponseFactory()
                .fromText(
                    "Response " + counter.incrementAndGet()
                )
                .build();

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            final Socket socket = connection.socket();

            send(
                socket,
                "GET /first HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "\r\n"
            );

            final TestResponse first = readResponse(socket);

            assertEquals(
                "HTTP/1.1 200 OK",
                first.statusLine()
            );
            assertEquals("Response 1", first.text());

            send(
                socket,
                "GET /second HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse second = readResponse(socket);

            assertEquals(
                "HTTP/1.1 200 OK",
                second.statusLine()
            );
            assertEquals("Response 2", second.text());

            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());
            assertEquals(2, counter.get());
        }
    }

    /**
     * Tests that several requests already present in the socket can be
     * processed through one persistent connection.
     */
    @Test
    void pipelinedKeepAlive() throws Exception {
        final Handler handler = (request, environment) ->
            environment
                .getResponseFactory()
                .fromText(request.getPath().getPath())
                .build();

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /first HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "\r\n"
                    + "GET /second HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse first =
                readResponse(connection.socket());

            final TestResponse second =
                readResponse(connection.socket());

            assertEquals("/first", first.text());
            assertEquals("/second", second.text());
        }
    }

    /**
     * Tests explicit connection closing in HTTP/1.1.
     */
    @Test
    void http11ConnectionClose() throws Exception {
        final Options options = options();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            readResponse(connection.socket());

            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());
            assertEquals(
                -1,
                connection.socket().getInputStream().read()
            );
        }
    }

    /**
     * Tests that HTTP/1.0 closes the connection by default.
     */
    @Test
    void http10ClosesByDefault() throws Exception {
        final Options options = options();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET / HTTP/1.0\r\n"
                    + "\r\n"
            );

            readResponse(connection.socket());

            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());
            assertEquals(
                -1,
                connection.socket().getInputStream().read()
            );
        }
    }

    /**
     * Tests explicit persistent connections in HTTP/1.0.
     */
    @Test
    void http10KeepAlive() throws Exception {
        final Handler handler = (request, environment) ->
            environment
                .getResponseFactory()
                .fromText(request.getPath().getPath())
                .build();

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /first HTTP/1.0\r\n"
                    + "Connection: keep-alive\r\n"
                    + "\r\n"
            );

            assertEquals(
                "/first",
                readResponse(connection.socket()).text()
            );

            send(
                connection.socket(),
                "GET /second HTTP/1.0\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            assertEquals(
                "/second",
                readResponse(connection.socket()).text()
            );
        }
    }

    /**
     * Tests normal client-side connection closing between HTTP requests.
     */
    @Test
    void clientClosesConnection() throws Exception {
        final Connection connection = connect(options());

        try {
            send(
                connection.socket(),
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "\r\n"
            );

            readResponse(connection.socket());

            /*
             * The worker is now waiting for another request.
             * Closing the client side must terminate it normally.
             */
            connection.socket().close();

            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());
        } finally {
            connection.close();
        }
    }

    /**
     * Tests closing an idle connection after the read timeout expires.
     */
    @Test
    void readTimeout() throws Exception {
        final Options options = new Options.Builder()
            .setReadTimeout(Duration.ofMillis(100))
            .build();

        try (Connection connection = connect(options)) {
            connection.worker().join(TIMEOUT);

            assertFalse(connection.worker().isAlive());

            final InputStream input =
                connection.socket().getInputStream();

            assertEquals(-1, input.read());
        }
    }

    /**
     * Creates options with a handler returning a simple successful response.
     *
     * @return
     *     the options.
     */
    private static Options options() {
        final Handler handler = (request, environment) ->
            environment
                .getResponseFactory()
                .fromText("OK")
                .build();

        return new Options.Builder()
            .setHandler(handler)
            .build();
    }
}
