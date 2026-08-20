/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Byte source backed by an input stream.
 */
final class InputStreamByteSource implements ByteSource {

    /**
     * The source stream.
     */
    private final InputStream input;

    /**
     * Creates a byte source.
     *
     * @param input
     *     the source stream.
     */
    InputStreamByteSource(final InputStream input) {
        this.input = input;
    }

    @Override
    public int read() throws ServerException {
        try {
            return input.read();
        } catch (IOException exception) {
            throw new ServerException(
                "Cannot read uploaded data",
                exception
            );
        }
    }
}
