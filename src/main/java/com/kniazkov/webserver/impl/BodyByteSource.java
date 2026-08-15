/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

/**
 * A byte source representing a request body with a known length.
 * <p>
 * The source exposes no more than the configured number of body bytes even if
 * the underlying connection contains additional data. This is important for
 * persistent HTTP connections, where bytes following the current body may
 * already belong to the next request.
 * <p>
 * Every byte read through this source is also accumulated so the original
 * request body can later be obtained as a single byte array.
 */
final class BodyByteSource implements ByteSource {

    /**
     * The underlying byte source.
     */
    private final ByteSource source;

    /**
     * The accumulated request body.
     */
    private final ByteAccumulator accumulator =
        new ByteAccumulator();

    /**
     * The number of body bytes remaining.
     */
    private long remaining;

    /**
     * Creates a request body byte source.
     *
     * @param source
     *     the underlying byte source.
     * @param length
     *     the exact request body length.
     */
    BodyByteSource(
        final ByteSource source,
        final long length
    ) {
        this.source = source;
        remaining = length;
    }

    /**
     * Reads the next body byte.
     *
     * @return
     *     the next byte in the range {@code 0..255}, or {@code -1} when the
     *     declared body length has been completely consumed.
     * @throws ServerException
     *     if the underlying source ends before the declared body length has
     *     been reached.
     */
    @Override
    public int read() throws ServerException {
        if (remaining == 0) {
            return -1;
        }

        final int value = source.read();

        if (value == -1) {
            throw new ServerException(
                "Unexpected end of HTTP request body"
            );
        }

        accumulator.append(value);
        remaining--;

        return value;
    }

    /**
     * Reads and discards all remaining body bytes.
     * <p>
     * The bytes are still accumulated and therefore remain available through
     * {@link #getData()}.
     *
     * @throws ServerException
     *     if the underlying source ends before the complete body is read.
     */
    void drain() throws ServerException {
        while (read() != -1) {
            // Intentionally empty.
        }
    }

    /**
     * Returns all body bytes read so far.
     *
     * @return
     *     the accumulated request body.
     */
    byte[] getData() {
        return accumulator.toByteArray();
    }
}
