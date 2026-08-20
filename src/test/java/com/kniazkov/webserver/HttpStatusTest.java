/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests lookup of HTTP statuses by numeric code.
 */
final class HttpStatusTest {

    /**
     * Tests lookup of known status codes.
     */
    @Test
    void fromCode() {
        for (HttpStatus status : HttpStatus.values()) {
            assertSame(
                status,
                HttpStatus.fromCode(status.getCode())
            );
        }

        assertEquals(
            HttpStatus.NOT_FOUND,
            HttpStatus.fromCode(404)
        );
    }

    /**
     * Tests rejection of a status absent from the enum.
     */
    @Test
    void unsupportedCode() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> HttpStatus.fromCode(999)
        );

        assertEquals(
            "Unsupported HTTP status code: 999",
            exception.getMessage()
        );
    }
}
