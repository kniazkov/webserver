/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.webserver;

/**
 * Structure describing a single file transferred using the POST method.
 */
public final class FileDescriptor {
    /**
     * Original file name.
     */
    public String name;

    /**
     * Content type, for example, {@code image/jpeg} or {@code text/html}.
     */
    public String contentType;

    /**
     * The file content.
     */
    public byte[] data;

    /**
     * Stringification (for debugging purposes).
     * @return File name as string representation
     */
    @Override
    public String toString() {
        return this.name;
    }
}
