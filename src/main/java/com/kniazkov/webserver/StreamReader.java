package com.kniazkov.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class StreamReader {
    private static final int BUFF_SIZE = 1024;

    private final InputStream stream;

    private final byte[] buff;

    private int offset;

    private int available;

    private int limit;

    private byte[] boundary;

    private byte[] data;

    StreamReader(final InputStream stream) {
        this.stream = stream;
        this.buff = new byte[BUFF_SIZE];
        this.offset = 0;
        this.available = 0;
        this.limit = -1; // unlimited
        this.boundary = null;
        this.data = null;
    }

    void setLimit(final int value) {
        limit = value;
        data = new byte[limit];
    }

    void setBoundary(final String value) {
        boundary = value.getBytes(StandardCharsets.US_ASCII);
    }

    String readLine() throws IOException {
        final StringBuilder builder = new StringBuilder();
        int ch = readByte();
        while (ch >= 0 && ch != 10) {
            builder.append((char)ch);
            ch = readByte();
        }
        return builder.toString().trim();
    }

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

    int readByte() throws IOException {
        if (limit == 0) {
            return -1;
        }
        if (available == 0) {
            available = stream.read(buff);
            if (available == 0) {
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
