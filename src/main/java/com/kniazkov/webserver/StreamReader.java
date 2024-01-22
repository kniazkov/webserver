package com.kniazkov.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class StreamReader {
    private static final int BUFF_SIZE = 1024;

    private final InputStream stream;

    private final byte[] buff;

    private int offset;

    private int available;

    private int limit;

    private byte[] boundary;

    StreamReader(InputStream stream) {
        this.stream = stream;
        this.buff = new byte[BUFF_SIZE];
        this.offset = 0;
        this.available = 0;
        this.limit = Integer.MAX_VALUE; // unlimited
        this.boundary = new byte[0];
    }

    void setLimit(int value) {
        limit = value;
    }

    void setBoundary(String value) {
        boundary = value.getBytes(StandardCharsets.US_ASCII);
    }

    String readLine() throws IOException {
        StringBuilder builder = new StringBuilder();
        int ch = read();
        while (ch >= 0 && ch != 10) {
            builder.append((char)ch);
            ch = read();
        }
        return builder.toString().trim();
    }

    String readLineToBoundary() throws IOException {
        if (boundary.length == 0) {
            return readLine();
        }
        StringBuilder builder = new StringBuilder();
        FixedSizeByteArray tail = new FixedSizeByteArray(boundary.length);
        int ch = read();
        while (ch >= 0 && ch != 10) {
            if (tail.full()) {
                builder.append((char)(tail.push((byte) ch)));
            } else {
                tail.push((byte) ch);
            }
            if (tail.equals(boundary)) {
                return builder.toString().trim();
            }
            ch = read();
        }
        builder.append(tail.toString());
        return builder.toString().trim();
    }

    private int read() throws IOException {
        if (limit < 1) {
            return -1;
        }
        if (available == 0) {
            available = stream.read(buff);
            if (available == 0) {
                return -1;
            }
            offset = 0;
        }
        limit--;
        available--;
        return (int)buff[offset++];
    }
}
