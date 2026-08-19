/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests parsing invalid HTTP request headers.
 */
final class RequestHeadersParserInvalidTest
    extends RequestHeadersParserBaseTest {

    /**
     * Tests an empty request.
     */
    @Test
    void emptyRequest() {
        assertInvalid("");
    }

    /**
     * Tests a request starting with an empty line.
     */
    @Test
    void emptyRequestLine() {
        assertInvalid("\r\n\r\n");
    }

    /**
     * Tests a request line without a method.
     */
    @Test
    void missingMethod() {
        assertInvalid(
            " / HTTP/1.1\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a request line without a target.
     */
    @Test
    void missingTarget() {
        assertInvalid(
            "GET  HTTP/1.1\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a request line without a version.
     */
    @Test
    void missingVersion() {
        assertInvalid(
            "GET /\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a request line with an empty version.
     */
    @Test
    void emptyVersion() {
        assertInvalid(
            "GET / \r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a request line containing too many components.
     */
    @Test
    void extraRequestLineComponent() {
        assertInvalid(
            "GET / HTTP/1.1 extra\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests multiple spaces between method and target.
     */
    @Test
    void multipleSpacesAfterMethod() {
        assertInvalid(
            "GET  / HTTP/1.1\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests multiple spaces between target and version.
     */
    @Test
    void multipleSpacesAfterTarget() {
        assertInvalid(
            "GET /  HTTP/1.1\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a leading space in the request line.
     */
    @Test
    void leadingSpace() {
        assertInvalid(
            " GET / HTTP/1.1\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a trailing space in the request line.
     */
    @Test
    void trailingSpace() {
        assertInvalid(
            "GET / HTTP/1.1 \r\n"
                + "\r\n"
        );
    }

    /**
     * Tests an unsupported HTTP method.
     */
    @Test
    void unsupportedMethod() {
        assertInvalid(
            "DELETE / HTTP/1.1\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests an unsupported HTTP version.
     */
    @Test
    void unsupportedVersion() {
        assertInvalid(
            "GET / HTTP/2\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a request line not terminated with CRLF.
     */
    @Test
    void incompleteRequestLine() {
        assertInvalid("GET / HTTP/1.1");
    }

    /**
     * Tests a request line terminated only with LF.
     */
    @Test
    void requestLineWithLoneLf() {
        assertInvalid(
            "GET / HTTP/1.1\n"
                + "\n"
        );
    }

    /**
     * Tests a request line terminated only with CR.
     */
    @Test
    void requestLineWithLoneCr() {
        assertInvalid("GET / HTTP/1.1\r");
    }

    /**
     * Tests a request without the empty line terminating the header section.
     */
    @Test
    void missingHeaderSectionTerminator() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "Host: example.com\r\n"
        );
    }

    /**
     * Tests a request without a header section terminator and without headers.
     */
    @Test
    void missingEmptyHeaderSectionTerminator() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
        );
    }

    /**
     * Tests a header without a colon.
     */
    @Test
    void headerWithoutColon() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "Host example.com\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a header with an empty name.
     */
    @Test
    void emptyHeaderName() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + ": example.com\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests whitespace before a header colon.
     */
    @Test
    void whitespaceBeforeColon() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "Host : example.com\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a tab before a header colon.
     */
    @Test
    void tabBeforeColon() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "Host\t: example.com\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests an invalid character in a header name.
     */
    @Test
    void invalidHeaderName() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "Content Type: text/plain\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a colon inside a header name.
     */
    @Test
    void colonAtBeginningOfHeaderValueOnly() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + ":value\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests an incomplete header line.
     */
    @Test
    void incompleteHeaderLine() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "Host: example.com"
        );
    }

    /**
     * Tests a header line terminated only with LF.
     */
    @Test
    void headerWithLoneLf() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "Host: example.com\n"
                + "\r\n"
        );
    }

    /**
     * Tests a carriage return inside a header line.
     */
    @Test
    void carriageReturnInsideHeader() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "Host: example\r.com\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests a line feed inside a header line.
     */
    @Test
    void lineFeedInsideHeader() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "Host: example\n.com\r\n"
                + "\r\n"
        );
    }

    /**
     * Tests an obsolete folded header.
     */
    @Test
    void foldedHeader() {
        assertInvalid(
            "GET / HTTP/1.1\r\n"
                + "X-Test: first\r\n"
                + " second\r\n"
                + "\r\n"
        );
    }

    /**
     * Verifies that parsing the specified request fails.
     *
     * @param request
     *     the HTTP request.
     */
    private static void assertInvalid(final String request) {
        assertThrows(
            ServerException.class,
            () -> parse(request)
        );
    }
}
