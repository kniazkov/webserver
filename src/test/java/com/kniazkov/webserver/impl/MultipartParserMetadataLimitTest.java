/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests multipart boundary and metadata limits.
 */
final class MultipartParserMetadataLimitTest
    extends MultipartParserBaseTest {

    /**
     * Tests the RFC 2046 maximum boundary length.
     */
    @Test
    void maximumBoundaryLength() throws ServerException {
        final String boundary = "b".repeat(70);
        final Request request = parse(
            body(boundary, "field", "value"),
            boundary,
            STANDARD_OPTIONS
        );

        assertEquals("value", request.getForm().get("field").getFirst());
    }

    /**
     * Tests rejection of a boundary longer than RFC 2046 permits.
     */
    @Test
    void boundaryTooLong() {
        final String boundary = "b".repeat(71);

        assertThrows(
            ServerException.class,
            () -> parse(
                body(boundary, "field", "value"),
                boundary,
                STANDARD_OPTIONS
            )
        );
    }

    /**
     * Tests rejection of a character outside the boundary grammar.
     */
    @Test
    void invalidBoundaryCharacter() {
        final String boundary = "invalid@boundary";

        assertThrows(
            ServerException.class,
            () -> parse(
                body(boundary, "field", "value"),
                boundary,
                STANDARD_OPTIONS
            )
        );
    }

    /**
     * Tests rejection of whitespace at the end of a boundary.
     */
    @Test
    void trailingBoundarySpace() {
        final String boundary = "invalid ";

        assertThrows(
            ServerException.class,
            () -> parse(
                body(boundary, "field", "value"),
                boundary,
                STANDARD_OPTIONS
            )
        );
    }

    /**
     * Tests that RFC 2046 permits whitespace inside a boundary.
     */
    @Test
    void internalBoundarySpace() throws ServerException {
        final String boundary = "valid boundary";
        final Request request = parse(
            body(boundary, "field", "value"),
            boundary,
            STANDARD_OPTIONS
        );

        assertEquals("value", request.getForm().get("field").getFirst());
    }

    /**
     * Tests a multipart body at the configured part count limit.
     */
    @Test
    void exactPartCountLimit() throws ServerException {
        final Options options = new Options.Builder()
            .setMaxMultipartParts(2)
            .build();

        final Request request = parse(
            twoPartBody(),
            options
        );

        assertEquals("one", request.getForm().get("first").getFirst());
        assertEquals("two", request.getForm().get("second").getFirst());
    }

    /**
     * Tests rejection of a body exceeding the part count limit.
     */
    @Test
    void partCountLimitExceeded() {
        final Options options = new Options.Builder()
            .setMaxMultipartParts(1)
            .build();

        assertPayloadTooLarge(
            () -> parse(twoPartBody(), options)
        );
    }

    /**
     * Tests a part header section at the configured byte limit.
     */
    @Test
    void exactPartHeaderLimit() throws ServerException {
        final String headers =
            "Content-Disposition: form-data; name=\"field\"\r\n"
                + "\r\n";

        final Options options = new Options.Builder()
            .setMaxMultipartHeaderSize(asciiLength(headers))
            .build();

        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + headers
                + "value\r\n"
                + "--" + BOUNDARY + "--",
            options
        );

        assertEquals("value", request.getForm().get("field").getFirst());
    }

    /**
     * Tests rejection of a part header section exceeding its byte limit.
     */
    @Test
    void partHeaderLimitExceeded() {
        final String headers =
            "Content-Disposition: form-data; name=\"field\"\r\n"
                + "\r\n";

        final Options options = new Options.Builder()
            .setMaxMultipartHeaderSize(asciiLength(headers) - 1)
            .build();

        assertPayloadTooLarge(
            () -> parse(
                "--" + BOUNDARY + "\r\n"
                    + headers
                    + "value\r\n"
                    + "--" + BOUNDARY + "--",
                options
            )
        );
    }

    /**
     * Creates a complete body containing one form field.
     *
     * @param boundary
     *     the multipart boundary.
     * @param name
     *     the form field name.
     * @param value
     *     the form field value.
     * @return
     *     the multipart body.
     */
    private static String body(
        final String boundary,
        final String name,
        final String value
    ) {
        return "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\""
            + name + "\"\r\n"
            + "\r\n"
            + value + "\r\n"
            + "--" + boundary + "--";
    }

    /**
     * Creates a complete body containing two form fields.
     *
     * @return
     *     the multipart body.
     */
    private static String twoPartBody() {
        return "--" + BOUNDARY + "\r\n"
            + "Content-Disposition: form-data; name=\"first\"\r\n"
            + "\r\n"
            + "one\r\n"
            + "--" + BOUNDARY + "\r\n"
            + "Content-Disposition: form-data; name=\"second\"\r\n"
            + "\r\n"
            + "two\r\n"
            + "--" + BOUNDARY + "--";
    }

    /**
     * Returns the ASCII byte length of a string.
     *
     * @param value
     *     the string.
     * @return
     *     the byte length.
     */
    private static int asciiLength(final String value) {
        return value.getBytes(StandardCharsets.US_ASCII).length;
    }

    /**
     * Verifies that parsing fails with HTTP 413.
     *
     * @param executable
     *     the parser invocation.
     */
    private static void assertPayloadTooLarge(
        final Executable executable
    ) {
        final ServerException exception = assertThrows(
            ServerException.class,
            executable
        );

        assertEquals(
            HttpStatus.PAYLOAD_TOO_LARGE,
            exception.getStatus().orElseThrow()
        );
    }
}
