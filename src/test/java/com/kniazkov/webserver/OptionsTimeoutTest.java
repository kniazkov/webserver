/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests connection and handler timeout options.
 */
final class OptionsTimeoutTest {

    /**
     * Tests explicit timeout configuration.
     */
    @Test
    void configuredTimeouts() {
        final Options options = new Options.Builder()
            .setReadTimeout(Duration.ofSeconds(1))
            .setWriteTimeout(Duration.ofSeconds(2))
            .setHandlerTimeout(Duration.ofSeconds(3))
            .build();

        assertEquals(
            Duration.ofSeconds(1),
            options.getReadTimeout()
        );
        assertEquals(
            Duration.ofSeconds(2),
            options.getWriteTimeout()
        );
        assertEquals(
            Duration.ofSeconds(3),
            options.getHandlerTimeout()
        );
    }

    /**
     * Tests rejection of invalid response write timeouts.
     */
    @Test
    void invalidWriteTimeout() {
        assertThrows(
            NullPointerException.class,
            () -> new Options.Builder().setWriteTimeout(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new Options.Builder()
                .setWriteTimeout(Duration.ZERO)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new Options.Builder()
                .setWriteTimeout(Duration.ofMillis(-1))
        );
    }
}
