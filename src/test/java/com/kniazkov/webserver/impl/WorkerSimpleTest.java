/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Options;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests successful processing of simple HTTP requests by {@link Worker}.
 */
final class WorkerSimpleTest extends WorkerBaseTest {

    /**
     * Tests returning raw bytes with a custom status and media type.
     */
    @Test
    void rawResponse() throws Exception {
        final byte[] body = {
            0, 1, (byte) 0xfe, (byte) 0xff
        };

        final Handler handler = (request, environment) ->
            environment
                .getResponseFactory()
                .custom(
                    HttpStatus.CREATED,
                    "application/vnd.example.packet",
                    body
                )
                .build();

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /packet HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 201")
            );
            assertEquals(
                "application/vnd.example.packet",
                response.header("Content-Type")
            );
            assertArrayEquals(
                body,
                response.body()
            );
        }
    }

    /**
     * Tests a simple GET request handled by a custom handler.
     */
    @Test
    void getRequest() throws Exception {
        final Handler handler = (request, environment) -> {
            assertEquals("/hello", request.getPath().getPath());
            assertEquals(
                Map.of("name", List.of("Ivan")),
                request.getQuery()
            );

            return environment
                .getResponseFactory()
                .fromText("Hello, Ivan!")
                .build();
        };

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /hello?name=Ivan HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 200")
            );
            assertEquals(
                "Hello, Ivan!",
                response.text()
            );
        }
    }

    /**
     * Tests a POST request with an arbitrary request body.
     */
    @Test
    void postRequest() throws Exception {
        final String body = "some request data";

        final Handler handler = (request, environment) -> {
            assertEquals("/submit", request.getPath().getPath());
            assertEquals(
                body,
                new String(
                    request.getBody(),
                    StandardCharsets.US_ASCII
                )
            );

            return environment
                .getResponseFactory()
                .fromJson("{\"result\":\"ok\"}")
                .build();
        };

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "POST /submit HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Type: application/octet-stream\r\n"
                    + "Content-Length: " + body.length() + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
                    + body
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 200")
            );
            assertEquals(
                "application/json",
                response.header("Content-Type")
            );
            assertEquals(
                "{\"result\":\"ok\"}",
                response.text()
            );
        }
    }

    /**
     * Tests a POST request containing a URL-encoded form.
     */
    @Test
    void postForm() throws Exception {
        final String body = "name=Ivan&language=Java";

        final Handler handler = (request, environment) -> {
            assertEquals(
                Map.of(
                    "name", List.of("Ivan"),
                    "language", List.of("Java")
                ),
                request.getForm()
            );

            return environment
                .getResponseFactory()
                .fromText("Saved")
                .build();
        };

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "POST /form HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Type: application/x-www-form-urlencoded\r\n"
                    + "Content-Length: " + body.length() + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
                    + body
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 200")
            );
            assertEquals("Saved", response.text());
        }
    }
}
