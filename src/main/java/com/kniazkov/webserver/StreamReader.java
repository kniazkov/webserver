/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Helper class for reading data from an incoming stream.
 */
final class StreamReader {
    /**
     * The size of the internal buffer into which data from the stream is saved before parsing.
     */
    private static final int BUFF_SIZE = 1024;

    /**
     * The input stream.
     */
    private final InputStream stream;

    /**
     * The internal buffer.
     */
    private final byte[] buff;

    /**
     * An offset from the beginning of the internal buffer that specifies the position
     * of the current byte.
     */
    private int offset;

    /**
     * Specifies how many bytes can still be read without accessing the stream.
     */
    private int available;

    /**
     * Specifies how many bytes can be read at all (<0 means there is no limit).
     */
    private int limit;

    /**
     * Specifies the boundary that separates the data.
     */
    private byte[] boundary;

    /**
     * Temporary buffer for reading data.
     * Since reading data is a synchronous process, the buffer is defined only once
     * so that a new one is not created for each call.
     */
    private byte[] data;

    /**
     * Constructor.
     * @param stream The input stream
     */
    StreamReader(final InputStream stream) {
        this.stream = stream;
        this.buff = new byte[BUFF_SIZE];
        this.offset = 0;
        this.available = 0;
        this.limit = -1; // unlimited
        this.boundary = null;
        this.data = null;
    }

    /**
     * Specifies the limit on how many bytes can be read.
     * @param value Limit
     */
    void setLimit(final int value) {
        limit = value;
        data = new byte[limit];
    }

    /**
     * Specifies the boundary that separates the data.
     * @param value Boundary
     */
    void setBoundary(final String value) {
        boundary = value.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Reads a string from the stream.
     * The string ends if the byte {code \n} is received or if the stream is empty.
     * @return String or empty string
     * @throws IOException If there's something wrong with the stream
     */
    String readLine() throws IOException {
        final StringBuilder builder = new StringBuilder();
        int ch = readByte();
        while (ch >= 0 && ch != 10) {
            builder.append((char)ch);
            ch = readByte();
        }
        return builder.toString().trim();
    }

    /**
     * Reads an array of bytes from the stream, until the stream ends
     * or until the boundary bytes are read.
     * @return Array of bytes or empty array
     * @throws IOException If there's something wrong with the stream
     */
    byte[] readArrayToBoundary() throws IOException {
        if (boundary == null || data == null) {
            return new byte[0];
        }
        int size = 0;
        int b = readByte();
        while (b >= 0) {
            data[size] = (byte)b;
            size++;
            if (size >= boundary.length) {
                boolean cut = true;
                for (int index = 0; index < boundary.length; index++) {
                    if (boundary[index] != data[size - boundary.length + index]) {
                        cut = false;
                        break;
                    }
                }
                if (cut) {
                    size = size - 2 /* CLRF */ - boundary.length;
                    break;
                }
            }
            b = readByte();
        }
        return Arrays.copyOf(data, size);
    }

    /**
     * Reads single byte from the stream.
     * @return Byte ASCII code of the byte or -1 if the stream is empty
     * @throws IOException If there's something wrong with the stream
     */
    int readByte() throws IOException {
        if (limit == 0) {
            return -1;
        }
        if (available == 0) {
            available = stream.read(buff);
            if (available < 0) {
                return -1;
            }
            offset = 0;
        }
        if (limit > 0) {
            limit--;
        }
        available--;
        return (int)((char)buff[offset++]);
    }
}
