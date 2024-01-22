package com.kniazkov.webserver;

import java.io.IOException;
import java.io.InputStream;

class StreamReader {
    private static final int BUFF_SIZE = 1024;

    private final InputStream stream;

    private final byte[] buff;

    private int offset;

    private int available;

    private int limit;

    StreamReader(InputStream stream) {
        this.stream = stream;
        this.buff = new byte[BUFF_SIZE];
        this.offset = 0;
        this.available = 0;
        this.limit = Integer.MAX_VALUE; // unlimited
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

    void setLimit(int value) {
        limit = value;
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
