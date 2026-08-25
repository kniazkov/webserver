/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests request upload storage options.
 */
final class OptionsUploadTest {

    /**
     * Tests explicit upload limits without silent adjustment.
     */
    @Test
    void limitsRemainIndependent() {
        final Options options = new Options.Builder()
            .setMaxRequestSize(1024)
            .setMaxFileSize(2048)
            .setMaxHeaderSize(4096)
            .setMaxInMemoryBodySize(128)
            .setMaxFormSize(256)
            .setMaxMultipartParts(12)
            .setMaxMultipartHeaderSize(512)
            .build();

        assertEquals(1024, options.getMaxRequestSize());
        assertEquals(2048, options.getMaxFileSize());
        assertEquals(4096, options.getMaxHeaderSize());
        assertEquals(128, options.getMaxInMemoryBodySize());
        assertEquals(256, options.getMaxFormSize());
        assertEquals(12, options.getMaxMultipartParts());
        assertEquals(512, options.getMaxMultipartHeaderSize());
    }

    /**
     * Tests rejection of negative multipart metadata limits.
     */
    @Test
    void negativeMultipartLimits() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Options.Builder().setMaxMultipartParts(-1)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new Options.Builder()
                .setMaxMultipartHeaderSize(-1)
        );
    }
}
