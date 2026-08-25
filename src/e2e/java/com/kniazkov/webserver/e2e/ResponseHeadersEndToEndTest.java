/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for generated response headers.
 */
final class ResponseHeadersEndToEndTest extends EndToEndBaseTest {

    /**
     * Tests that a real client receives the declared UTF-8 character set.
     */
    @Test
    void utf8TextCharset() throws Exception {
        startServer(
            (request, environment) -> environment
                .getResponseFactory()
                .fromText("Привет")
                .build()
        );

        final var response = page.request().get(url("/text"));

        assertEquals(200, response.status());
        assertEquals(
            "text/plain; charset=UTF-8",
            response.headers().get("content-type")
        );
        assertEquals("Привет", response.text());
    }
}
