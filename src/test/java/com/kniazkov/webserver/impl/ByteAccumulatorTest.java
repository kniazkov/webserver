/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link ByteAccumulator}.
 */
final class ByteAccumulatorTest {

    /**
     * Tests an empty accumulator.
     */
    @Test
    void empty() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        assertEquals(0, accumulator.size());
        assertArrayEquals(new byte[0], accumulator.toByteArray());
        assertFalse(accumulator.endsWith(bytes("test")));
        assertTrue(accumulator.endsWith(new byte[0]));
    }

    /**
     * Tests accumulating bytes.
     */
    @Test
    void append() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        accumulator.append('a');
        accumulator.append('b');
        accumulator.append('c');

        assertEquals(3, accumulator.size());
        assertArrayEquals(bytes("abc"), accumulator.toByteArray());
    }

    /**
     * Tests data that exactly fills one chunk.
     */
    @Test
    void fullChunk() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        for (int index = 0; index < 1024; index++) {
            accumulator.append(index);
        }

        assertEquals(1024, accumulator.size());

        final byte[] data = accumulator.toByteArray();
        assertEquals(1024, data.length);

        for (int index = 0; index < data.length; index++) {
            assertEquals((byte) index, data[index]);
        }
    }

    /**
     * Tests data spanning several chunks.
     */
    @Test
    void severalChunks() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        for (int index = 0; index < 2500; index++) {
            accumulator.append(index);
        }

        assertEquals(2500, accumulator.size());

        final byte[] data = accumulator.toByteArray();
        assertEquals(2500, data.length);

        for (int index = 0; index < data.length; index++) {
            assertEquals((byte) index, data[index]);
        }
    }

    /**
     * Tests suffix matching inside the last chunk.
     */
    @Test
    void endsWith() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        for (byte value : bytes("Hello, world!")) {
            accumulator.append(value);
        }

        assertTrue(accumulator.endsWith(bytes("world!")));
        assertTrue(accumulator.endsWith(bytes("Hello, world!")));
        assertFalse(accumulator.endsWith(bytes("World!")));
        assertFalse(accumulator.endsWith(bytes("Hello, world!!")));
    }

    /**
     * Tests suffix matching across a chunk boundary.
     */
    @Test
    void endsWithAcrossChunkBoundary() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        for (int index = 0; index < 1022; index++) {
            accumulator.append('x');
        }

        for (byte value : bytes("boundary")) {
            accumulator.append(value);
        }

        assertTrue(accumulator.endsWith(bytes("boundary")));
        assertFalse(accumulator.endsWith(bytes("xboundaryy")));
    }

    /**
     * Tests removing bytes from the last chunk.
     */
    @Test
    void removeLast() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        for (byte value : bytes("Hello, world!")) {
            accumulator.append(value);
        }

        accumulator.removeLast(7);

        assertEquals(6, accumulator.size());
        assertArrayEquals(bytes("Hello,"), accumulator.toByteArray());
    }

    /**
     * Tests removing bytes across a chunk boundary.
     */
    @Test
    void removeAcrossChunkBoundary() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        for (int index = 0; index < 1100; index++) {
            accumulator.append(index);
        }

        accumulator.removeLast(100);

        assertEquals(1000, accumulator.size());

        final byte[] data = accumulator.toByteArray();
        assertEquals(1000, data.length);

        for (int index = 0; index < data.length; index++) {
            assertEquals((byte) index, data[index]);
        }
    }

    /**
     * Tests removing complete chunks.
     */
    @Test
    void removeSeveralChunks() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        for (int index = 0; index < 2500; index++) {
            accumulator.append(index);
        }

        accumulator.removeLast(2000);

        assertEquals(500, accumulator.size());

        final byte[] data = accumulator.toByteArray();

        for (int index = 0; index < data.length; index++) {
            assertEquals((byte) index, data[index]);
        }
    }

    /**
     * Tests removing all accumulated data.
     */
    @Test
    void removeAll() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        for (int index = 0; index < 2500; index++) {
            accumulator.append(index);
        }

        accumulator.removeLast(accumulator.size());

        assertEquals(0, accumulator.size());
        assertArrayEquals(new byte[0], accumulator.toByteArray());
    }

    /**
     * Tests appending data after removing bytes.
     */
    @Test
    void appendAfterRemove() {
        final ByteAccumulator accumulator = new ByteAccumulator();

        for (byte value : bytes("Hello, world!")) {
            accumulator.append(value);
        }

        accumulator.removeLast(6);

        for (byte value : bytes("Java!")) {
            accumulator.append(value);
        }

        assertArrayEquals(
            bytes("Hello, Java!"),
            accumulator.toByteArray()
        );
    }

    /**
     * Tests invalid removal sizes.
     */
    @Test
    void invalidRemove() {
        final ByteAccumulator accumulator = new ByteAccumulator();
        accumulator.append('a');

        assertThrows(
            IllegalArgumentException.class,
            () -> accumulator.removeLast(-1)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> accumulator.removeLast(2)
        );
    }

    /**
     * Converts a string to bytes.
     *
     * @param value
     *     the string.
     * @return
     *     the bytes.
     */
    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
