/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for large response bodies.
 */
final class LargeDataEndToEndTest
    extends EndToEndBaseTest {

    /**
     * Tests serving a large binary static file without data corruption.
     */
    @Test
    void largeBinaryFile() throws Exception {
        final byte[] data =
            new byte[5 * 1024 * 1024 + 123];

        for (int index = 0; index < data.length; index++) {
            data[index] = (byte) (index * 31);
        }

        writeFile(
            "large.bin",
            data
        );

        startServer();

        final var response =
            page.request().get(url("/large.bin"));

        assertEquals(200, response.status());
        assertArrayEquals(data, response.body());
    }
}
