package com.kniazkov.webserver.impl;

/**
 * A byte source backed by a byte array for testing purposes.
 */
final class ByteArrayByteSource implements ByteSource {

    /**
     * The source data.
     */
    private final byte[] data;

    /**
     * The current position.
     */
    private int position;


    /**
     * Creates a byte source from the specified array.
     *
     * @param data
     *     the source data.
     */
    ByteArrayByteSource(final byte[] data) {
        this.data = data;
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
