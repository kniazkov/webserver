/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests request header parsing when the underlying byte source fails.
 */
final class RequestHeadersParserSourceFailureTest {

    /**
     * Tests an unexpected source failure while parsing a valid request.
     */
    @Test
    void sourceFailure() {
        final ByteSource byteSource = new FailingByteSource(
            "GET / HTTP/1.1\r\n"
                + "Host: example.com\r\n"
                + "Accept: text/plain\r\n"
                + "\r\n",
            25
        );
        final StringSource stringSource = new StringSource(byteSource);

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> RequestHeadersParser.parse(stringSource)
        );

        assertEquals("Unexpected source failure", exception.getMessage());
    }

    /**
     * A byte source that fails after a specified number of bytes.
     */
    private static final class FailingByteSource implements ByteSource {

        /**
         * The source data.
         */
        private final byte[] data;

        /**
         * The position at which reading fails.
         */
        private final int failurePosition;

        /**
         * The current position.
         */
        private int position;

        /**
         * Creates a failing byte source.
         *
         * @param value
         *     the source data.
         * @param failurePosition
         *     the position at which reading fails.
         */
        private FailingByteSource(
            final String value,
            final int failurePosition
        ) {
            data = value.getBytes(StandardCharsets.US_ASCII);
            this.failurePosition = failurePosition;
        }

        /**
         * Reads the next byte or fails at the configured position.
         *
         * @return
         *     the next byte.
         * @throws ServerException
         *     when the failure position is reached.
         */
        @Override
        public int read() throws ServerException {
            if (position == failurePosition) {
                throw new ServerException("Unexpected source failure");
            }

            if (position == data.length) {
                return -1;
            }

            return data[position++] & 0xff;
        }
    }
}
