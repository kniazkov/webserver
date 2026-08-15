/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

/**
 * A byte source that limits the total number of bytes read from another source.
 * <p>
 * This class is used while parsing a complete HTTP request. Every successfully
 * read byte is counted, and reading fails once the configured request size
 * limit is exceeded.
 * <p>
 * The end-of-stream marker {@code -1} is not counted as a byte.
 */
final class RequestByteSource implements ByteSource {

    /**
     * The underlying byte source.
     */
    private final ByteSource source;

    /**
     * The maximum number of bytes that may be read.
     */
    private final long limit;

    /**
     * The number of bytes read.
     */
    private long count;

    /**
     * Creates a limited request byte source.
     *
     * @param source
     *     the underlying byte source.
     * @param limit
     *     the maximum number of bytes that may be read.
     */
    RequestByteSource(
        final ByteSource source,
        final long limit
    ) {
        this.source = source;
        this.limit = limit;
    }

    /**
     * Reads the next byte and updates the total byte count.
     *
     * @return
     *     the next byte in the range {@code 0..255}, or {@code -1} if the
     *     underlying source has been exhausted.
     * @throws ServerException
     *     if the underlying source fails or the configured size limit is
     *     exceeded.
     */
    @Override
    public int read() throws ServerException {
        final int value = source.read();

        if (value == -1) {
            if (count == 0) {
                throw new ConnectionClosedException();
            }

            return -1;
        }

        count++;

        if (count > limit) {
            throw new ServerException(
                "Maximum HTTP request size exceeded"
            );
        }

        return value;
    }
}
