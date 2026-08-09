/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests parsing invalid multipart form data.
 */
final class MultipartParserInvalidTest extends MultipartParserBaseTest {

    /**
     * Tests a missing initial boundary.
     */
    @Test
    void missingInitialBoundary() {
        assertInvalid(
            "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests an incorrect initial boundary.
     */
    @Test
    void invalidInitialBoundary() {
        assertInvalid(
            "--wrong-boundary\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests an incomplete initial boundary.
     */
    @Test
    void incompleteInitialBoundary() {
        assertInvalid(
            "--" + BOUNDARY
        );
    }

    /**
     * Tests a missing CRLF after the initial boundary.
     */
    @Test
    void missingCrlfAfterInitialBoundary() {
        assertInvalid(
            "--" + BOUNDARY
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests a part without Content-Disposition.
     */
    @Test
    void missingContentDisposition() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests an invalid multipart header.
     */
    @Test
    void invalidPartHeader() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests Content-Disposition with an invalid type.
     */
    @Test
    void invalidDispositionType() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: attachment; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests Content-Disposition without a field name.
     */
    @Test
    void missingFieldName() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests Content-Disposition with an empty field name.
     */
    @Test
    void emptyFieldName() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests a malformed Content-Disposition parameter.
     */
    @Test
    void invalidDispositionParameter() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests an unexpected end while reading part headers.
     */
    @Test
    void unexpectedEndInHeaders() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
        );
    }

    /**
     * Tests an unexpected end while reading part data.
     */
    @Test
    void unexpectedEndInData() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan"
        );
    }

    /**
     * Tests an invalid boundary suffix.
     */
    @Test
    void invalidBoundarySuffix() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "xx"
        );
    }

    /**
     * Tests an invalid line ending in part headers.
     */
    @Test
    void invalidHeaderLineEnding() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\n"
                + "\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n"
        );
    }

    /**
     * Tests a carriage return not followed by a line feed.
     */
    @Test
    void invalidCarriageReturnInHeaders() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\rX"
        );
    }

    /**
     * Tests trailing garbage after the final boundary.
     */
    @Test
    void garbageAfterFinalBoundary() {
        assertInvalid(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--garbage"
        );
    }

    /**
     * Tests a file exceeding the configured maximum size.
     */
    @Test
    void fileTooLarge() {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"test.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + "123456\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Options options = new Options.Builder()
            .setMaxFileSize(5)
            .build();

        assertThrows(
            ServerException.class,
            () -> parse(body, options)
        );
    }

    /**
     * Verifies that parsing the specified multipart body fails.
     *
     * @param body
     *     the multipart body.
     */
    private static void assertInvalid(final String body) {
        assertThrows(
            ServerException.class,
            () -> parse(body)
        );
    }
}
