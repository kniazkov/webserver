/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link StringSource}.
 */
final class StringSourceTest {
    /**
     * Options for test purposes.
     */
    private static final Options OPTIONS = new Options.Builder().build();

    /**
     * Tests reading a single line.
     */
    @Test
    void singleLine() throws ServerException {
        final StringSource source = new StringSource(
            new StringByteSource("Hello\r\n"),
            OPTIONS
        );

        assertEquals("Hello", source.read());
        assertNull(source.read());
    }

    /**
     * Tests reading several lines.
     */
    @Test
    void severalLines() throws ServerException {
        final StringSource source = new StringSource(
            new StringByteSource(
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Accept: text/plain\r\n"
            ),
            OPTIONS
        );

        assertEquals("GET / HTTP/1.1", source.read());
        assertEquals("Host: localhost", source.read());
        assertEquals("Accept: text/plain", source.read());
        assertNull(source.read());
    }

    /**
     * Tests reading an empty line.
     */
    @Test
    void emptyLine() throws ServerException {
        final StringSource source = new StringSource(
            new StringByteSource("\r\n"),
            OPTIONS
        );

        assertEquals("", source.read());
        assertNull(source.read());
    }

    /**
     * Tests reading several empty lines.
     */
    @Test
    void severalEmptyLines() throws ServerException {
        final StringSource source = new StringSource(
            new StringByteSource("\r\n\r\n\r\n"),
            OPTIONS
        );

        assertEquals("", source.read());
        assertEquals("", source.read());
        assertEquals("", source.read());
        assertNull(source.read());
    }

    /**
     * Tests an empty byte source.
     */
    @Test
    void emptySource() throws ServerException {
        final StringSource source = new StringSource(
            new StringByteSource(""),
            OPTIONS
        );

        assertNull(source.read());
        assertNull(source.read());
    }

    /**
     * Tests an unexpected end of source in the middle of a line.
     */
    @Test
    void incompleteLine() {
        final StringSource source = new StringSource(
            new StringByteSource("Hello"),
            OPTIONS
        );

        assertThrows(ServerException.class, source::read);
    }

    /**
     * Tests an unexpected end of source after a carriage return.
     */
    @Test
    void incompleteLineEnding() {
        final StringSource source = new StringSource(
            new StringByteSource("Hello\r"),
            OPTIONS
        );

        assertThrows(ServerException.class, source::read);
    }

    /**
     * Tests a line ending with a lone line feed.
     */
    @Test
    void loneLineFeed() {
        final StringSource source = new StringSource(
            new StringByteSource("Hello\n"),
            OPTIONS
        );

        assertThrows(ServerException.class, source::read);
    }

    /**
     * Tests a line feed inside a line.
     */
    @Test
    void lineFeedInsideLine() {
        final StringSource source = new StringSource(
            new StringByteSource("Hello\nWorld\r\n"),
            OPTIONS
        );

        assertThrows(ServerException.class, source::read);
    }

    /**
     * Tests a carriage return not followed by a line feed.
     */
    @Test
    void invalidCarriageReturn() {
        final StringSource source = new StringSource(
            new StringByteSource("Hello\rWorld\r\n"),
            OPTIONS
        );

        assertThrows(ServerException.class, source::read);
    }

    /**
     * Tests that valid lines before an invalid line can still be read.
     */
    @Test
    void invalidLaterLine() throws ServerException {
        final StringSource source = new StringSource(
            new StringByteSource(
                "First\r\n"
                    + "Second\r\n"
                    + "Broken"
            ),
            OPTIONS
        );

        assertEquals("First", source.read());
        assertEquals("Second", source.read());
        assertThrows(ServerException.class, source::read);
    }

    /**
     * Tests reading spaces and horizontal tabs as ordinary characters.
     */
    @Test
    void whitespace() throws ServerException {
        final StringSource source = new StringSource(
            new StringByteSource("  Hello\tworld  \r\n"),
            OPTIONS
        );

        assertEquals("  Hello\tworld  ", source.read());
        assertNull(source.read());
    }

    /**
     * Tests exceeding the maximum total header size.
     */
    @Test
    void maximumSizeExceeded() throws ServerException {
        final Options options = new Options.Builder()
            .setMaxHeaderSize(10)
            .build();

        final StringSource source = new StringSource(
            new StringByteSource(
                "1234\r\n"
                    + "5678\r\n"
            ),
            options
        );

        assertEquals("1234", source.read());
        assertThrows(ServerException.class, source::read);
    }
}
