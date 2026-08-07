/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

/**
 * Reads CRLF-terminated strings from a byte source.
 */
final class StringSource {

    /**
     * The underlying byte source.
     */
    private final ByteSource source;

    /**
     * Creates a string source.
     *
     * @param source
     *     the underlying byte source.
     */
    StringSource(final ByteSource source) {
        this.source = source;
    }

    /**
     * Reads the next CRLF-terminated string.
     *
     * @return
     *     the next string without the terminating CRLF sequence,
     *     or {@code null} if the end of the source has been reached.
     * @throws ServerException
     *     if the source contains an invalid or incomplete line.
     */
    String read() throws ServerException {
        final StringBuilder builder = new StringBuilder();

        while (true) {
            final int value = source.read();

            if (value == -1) {
                if (builder.isEmpty()) {
                    return null;
                }
                throw new ServerException("Unexpected end of HTTP line");
            }

            if (value == Lexer.LF) {
                throw new ServerException("Invalid HTTP line ending");
            }

            if (value == Lexer.CR) {
                final int next = source.read();

                if (next == -1) {
                    throw new ServerException("Unexpected end of HTTP line");
                }

                if (next != Lexer.LF) {
                    throw new ServerException("Invalid HTTP line ending");
                }

                return builder.toString();
            }

            builder.append((char) value);
        }
    }
}
