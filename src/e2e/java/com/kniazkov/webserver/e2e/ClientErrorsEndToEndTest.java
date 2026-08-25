/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Options;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for protocol-level client errors.
 */
final class ClientErrorsEndToEndTest extends EndToEndBaseTest {

    /**
     * Maximum request size used by this test server.
     */
    private static final int MAX_REQUEST_SIZE = 768;

    /**
     * Configures a deliberately small complete-request limit.
     *
     * @param builder
     *     the server options builder.
     */
    @Override
    protected void configure(final Options.Builder builder) {
        builder
            .setMaxHeaderSize(512)
            .setMaxRequestSize(MAX_REQUEST_SIZE);
        super.configure(builder);
    }

    /**
     * Tests that an unsupported method produces a safe HTTP 501 response.
     */
    @Test
    void unsupportedMethod() throws Exception {
        startServer();

        final HttpResponse<String> response = send(
            HttpRequest.newBuilder(URI.create(url("/")))
                .method(
                    "DIAGNOSTIC-METHOD",
                    HttpRequest.BodyPublishers.noBody()
                )
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(501, response.statusCode());
        assertTrue(response.body().contains("Not Implemented"));
        assertFalse(response.body().contains("DIAGNOSTIC-METHOD"));
    }

    /**
     * Tests that a request exceeding the configured limit produces HTTP 413
     * without exposing the internal validation message.
     */
    @Test
    void payloadTooLarge() throws Exception {
        startServer();

        final HttpResponse<String> response = send(
            HttpRequest.newBuilder(URI.create(url("/upload")))
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "x".repeat(MAX_REQUEST_SIZE * 2)
                    )
                )
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(413, response.statusCode());
        assertTrue(response.body().contains("Payload Too Large"));
        assertFalse(
            response.body().contains(
                "Maximum HTTP request size exceeded"
            )
        );
    }

    /**
     * Tests that an unknown media type remains available as raw uploaded data.
     */
    @Test
    void unknownMediaTypeIsRawData() throws Exception {
        final byte[] data = {
            0, 1, 2, 3, (byte) 0xff
        };

        startServer(
            (request, environment) -> environment
                .getResponseFactory()
                .fromBytes(request.getBody().readAllBytes())
                .build()
        );

        final HttpResponse<byte[]> response = send(
            HttpRequest.newBuilder(URI.create(url("/upload")))
                .header(
                    "Content-Type",
                    "application/x-diagnostic-binary"
                )
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(200, response.statusCode());
        assertArrayEquals(data, response.body());
    }

    /**
     * Sends one request to the test server.
     *
     * @param request
     *     the request.
     * @param handler
     *     the response body handler.
     * @param <T>
     *     the response body type.
     * @return
     *     the response.
     * @throws Exception
     *     if the request cannot be sent.
     */
    private static <T> HttpResponse<T> send(
        final HttpRequest request,
        final HttpResponse.BodyHandler<T> handler
    ) throws Exception {
        return HttpClient
            .newHttpClient()
            .send(request, handler);
    }
}
