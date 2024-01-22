package com.kniazkov.webserver;

class FixedSizeByteArray {
    private final byte[] buff;

    private int size;

    private int begin;

    FixedSizeByteArray(final int capacity) {
         buff = new byte[capacity];
         size = 0;
         begin = 0;
    }

    byte push(final byte value) {
        byte old = buff[begin];
        buff[begin] = value;
        begin++;
        if (begin == buff.length) {
            begin = 0;
        }
        if (size < buff.length) {
            size++;
        }
        return old;
    }

    boolean full() {
        return size == buff.length;
    }

    byte get(final int index) {
        if (size < buff.length) {
            return buff[index];
        }
        int offset = index + begin;
        if (offset >= buff.length) {
            offset -= buff.length;
        }
        return buff[offset];
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof byte[])) {
            return false;
        }
        byte[] array = (byte[]) obj;
        if (array.length != size) {
            return false;
        }
        for (int index = 0; index < size; index++) {
            if (array[index] != get(index)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(size);
        for (int index = 0; index < size; index++) {
            builder.append((char)(get(index)));
        }
        return builder.toString();
    }
}
