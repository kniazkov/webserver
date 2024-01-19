package com.kniazkov.webserver;

import java.io.IOException;
import java.io.InputStream;

class StreamReader {
    private static final int BUFF_SIZE = 1024;

    private final InputStream stream;

    private final byte[] buff;

    private int offset;

    private int available;

    private int count;

    StreamReader(InputStream stream) {
        this.stream = stream;
        this.buff = new byte[BUFF_SIZE];
        this.offset = 0;
        this.available = 0;
        this.count = 0;
    }

    String readLine() throws IOException {
        StringBuilder builder = new StringBuilder();
        int ch = read();
        while (ch >= 0 && ch != 13) {
            builder.append((char)ch);
            ch = read();
        }
        return builder.toString().trim();
    }

    int getCount() {
        return count;
    }

    private int read() throws IOException {
        if (available == 0) {
            available = stream.read(buff);
            if (available == 0) {
                return -1;
            }
            offset = 0;
        }
        count++;
        available--;
        return (int)buff[offset++];
    }
}
