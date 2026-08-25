/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests strict HTTP method parsing.
 */
final class HttpMethodTest {

    /**
     * Tests exact case-sensitive lookup of supported methods.
     */
    @Test
    void supportedMethods() throws ServerException {
        assertSame(HttpMethod.GET, HttpMethod.fromString("GET"));
        assertSame(HttpMethod.POST, HttpMethod.fromString("POST"));
    }

    /**
     * Tests that a syntactically valid but unsupported method is not silently
     * normalized to a supported one.
     */
    @Test
    void caseSensitiveMethod() {
        final ServerException exception = assertThrows(
            ServerException.class,
            () -> HttpMethod.fromString("get")
        );

        assertEquals(
            HttpStatus.NOT_IMPLEMENTED,
            exception.getStatus().orElseThrow()
        );
    }

    /**
     * Tests rejection of characters outside the HTTP token grammar.
     */
    @Test
    void invalidMethodToken() {
        final ServerException exception = assertThrows(
            ServerException.class,
            () -> HttpMethod.fromString("G@T")
        );

        assertFalse(exception.getStatus().isPresent());
    }
}
