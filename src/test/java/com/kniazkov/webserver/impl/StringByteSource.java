/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import java.nio.charset.StandardCharsets;

/**
 * A byte source backed by a string for testing purposes.
 */
final class StringByteSource implements ByteSource {

    /**
     * The source data.
     */
    private final byte[] data;

    /**
     * The current position.
     */
    private int position;

    /**
     * Creates a byte source from the specified string.
     *
     * @param value
     *     the source string.
     */
    StringByteSource(final String value) {
        data = value.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Reads the next byte from the source.
     *
     * @return
     *     the next byte, or {@code -1} if the end of the source has been
     *     reached.
     */
    @Override
    public int read() {
        if (position == data.length) {
            return -1;
        }
        return data[position++] & 0xff;
    }
}
