/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Uploaded data backed by one in-memory byte array.
 */
final class MemoryUploadedData implements StoredUploadedData {

    /**
     * The shared request data.
     */
    private final byte[] data;

    /**
     * The first byte in this view.
     */
    private final int offset;

    /**
     * The number of bytes in this view.
     */
    private final int length;

    /**
     * Creates an in-memory data object.
     *
     * @param data
     *     the complete request data.
     */
    MemoryUploadedData(final byte[] data) {
        this(data, 0, data.length);
    }

    /**
     * Creates a view over an in-memory data object.
     *
     * @param data
     *     the shared request data.
     * @param offset
     *     the view offset.
     * @param length
     *     the view length.
     */
    private MemoryUploadedData(
        final byte[] data,
        final int offset,
        final int length
    ) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public long getSize() {
        return length;
    }

    @Override
    public InputStream openStream() {
        return new ByteArrayInputStream(data, offset, length);
    }

    @Override
    public StoredUploadedData slice(
        final long value,
        final long size
    ) {
        validateRange(value, size);
        return new MemoryUploadedData(
            data,
            offset + (int) value,
            (int) size
        );
    }

    @Override
    public void close() {
        /*
         * No external resources.
         */
    }

    /**
     * Reads an exact number of bytes into memory.
     *
     * @param source
     *     the source.
     * @param length
     *     the exact length.
     * @return
     *     the uploaded data.
     * @throws ServerException
     *     if the source ends early.
     */
    static MemoryUploadedData read(
        final ByteSource source,
        final int length
    ) throws ServerException {
        final byte[] result = new byte[length];

        for (int index = 0; index < result.length; index++) {
            final int value = source.read();

            if (value == -1) {
                throw new ServerException(
                    "Unexpected end of HTTP request body"
                );
            }

            result[index] = (byte) value;
        }

        return new MemoryUploadedData(result);
    }

    /**
     * Validates a requested view range.
     *
     * @param value
     *     the offset.
     * @param size
     *     the length.
     */
    private void validateRange(
        final long value,
        final long size
    ) {
        if (
            value < 0
                || size < 0
                || value > length
                || size > length - value
        ) {
            throw new IllegalArgumentException(
                "Uploaded data range is outside the request body"
            );
        }
    }
}
