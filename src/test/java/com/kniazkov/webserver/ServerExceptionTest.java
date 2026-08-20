/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests HTTP status metadata carried by {@link ServerException}.
 */
final class ServerExceptionTest {

    /**
     * Tests that a legacy exception remains an internal error.
     */
    @Test
    void internalErrorHasNoStatus() {
        final ServerException exception =
            new ServerException("Database password leaked");

        assertTrue(exception.getStatus().isEmpty());
    }

    /**
     * Tests an exception with a client-visible HTTP status.
     */
    @Test
    void carriesHttpStatus() {
        final ServerException exception = new ServerException(
            HttpStatus.CONFLICT,
            "Resource already exists"
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatus().orElseThrow()
        );
        assertEquals(
            "Resource already exists",
            exception.getMessage()
        );
    }
}
