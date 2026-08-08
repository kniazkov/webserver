/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Options;
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
     * The maximum number of bytes that may be read.
     */
    private final long maxSize;

    /**
     * The number of bytes read from the source.
     */
    private long bytesRead;

    /**
     * Creates a string source.
     *
     * @param source
     *     the underlying byte source.
     * @param options
     *     the server options.
     */
    StringSource(
        final ByteSource source,
        final Options options
    ) {
        this.source = source;
        maxSize = options.getMaxHeaderSize();
    }

    /**
     * Reads the next CRLF-terminated string.
     *
     * @return
     *     the next string without the terminating CRLF sequence,
     *     or {@code null} if the end of the source has been reached.
     * @throws ServerException
     *     if the source contains an invalid or incomplete line, or the maximum
     *     header size is exceeded.
     */
    String read() throws ServerException {
        final StringBuilder builder = new StringBuilder();

        while (true) {
            final int value = readByte();

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
                final int next = readByte();

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

    /**
     * Reads the next byte and checks the total size limit.
     *
     * @return
     *     the next byte, or {@code -1} if the source has been exhausted.
     * @throws ServerException
     *     if the source cannot be read or the maximum header size is exceeded.
     */
    private int readByte() throws ServerException {
        final int value = source.read();

        if (value != -1) {
            bytesRead++;

            if (bytesRead > maxSize) {
                throw new ServerException(
                    "Maximum HTTP header size exceeded"
                );
            }
        }

        return value;
    }
}
