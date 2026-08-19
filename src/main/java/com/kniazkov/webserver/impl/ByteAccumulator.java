/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates bytes in fixed-size chunks.
 */
final class ByteAccumulator {

    /**
     * The chunk size.
     */
    private static final int CHUNK_SIZE = 1024;

    /**
     * The chunks containing accumulated data.
     */
    private final List<byte[]> chunks = new ArrayList<>();

    /**
     * The number of used bytes in the last chunk.
     */
    private int tail;

    /**
     * Creates an empty byte accumulator.
     */
    ByteAccumulator() {
        chunks.add(new byte[CHUNK_SIZE]);
    }

    /**
     * Adds a byte to the accumulator.
     *
     * @param value
     *     the byte value.
     */
    void append(final int value) {
        if (tail == CHUNK_SIZE) {
            chunks.add(new byte[CHUNK_SIZE]);
            tail = 0;
        }

        chunks.get(chunks.size() - 1)[tail++] = (byte) value;
    }

    /**
     * Returns the number of accumulated bytes.
     *
     * @return
     *     the number of bytes.
     */
    int size() {
        return (chunks.size() - 1) * CHUNK_SIZE + tail;
    }

    /**
     * Returns whether the accumulated data ends with the specified sequence.
     *
     * @param pattern
     *     the byte sequence.
     * @return
     *     {@code true} if the accumulated data ends with the sequence.
     */
    boolean endsWith(final byte[] pattern) {
        if (pattern.length > size()) {
            return false;
        }

        int chunkIndex = chunks.size() - 1;
        int offset = tail - pattern.length;

        while (offset < 0) {
            chunkIndex--;
            offset += CHUNK_SIZE;
        }

        for (byte value : pattern) {
            if (chunks.get(chunkIndex)[offset] != value) {
                return false;
            }

            offset++;

            if (offset == CHUNK_SIZE) {
                chunkIndex++;
                offset = 0;
            }
        }

        return true;
    }

    /**
     * Removes the specified number of bytes from the end.
     *
     * @param count
     *     the number of bytes to remove.
     * @throws IllegalArgumentException
     *     if the count is negative or greater than the current size.
     */
    void removeLast(final int count) {
        if (count < 0 || count > size()) {
            throw new IllegalArgumentException("Invalid byte count: " + count);
        }

        tail -= count;

        while (tail < 0) {
            chunks.remove(chunks.size() - 1);
            tail += CHUNK_SIZE;
        }
    }

    /**
     * Returns all accumulated data as a single byte array.
     *
     * @return
     *     the accumulated bytes.
     */
    byte[] toByteArray() {
        final byte[] result = new byte[size()];
        int offset = 0;

        for (int index = 0; index < chunks.size() - 1; index++) {
            System.arraycopy(
                chunks.get(index),
                0,
                result,
                offset,
                CHUNK_SIZE
            );
            offset += CHUNK_SIZE;
        }

        System.arraycopy(
            chunks.get(chunks.size() - 1),
            0,
            result,
            offset,
            tail
        );

        return result;
    }
}
