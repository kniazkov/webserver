/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies strict parsing of complete HTTP/1.x request heads.
 */
final class RequestHeaderParserTest {
    @Test
    void parseReturnsAllRequestLinePartsAndPreservesRepeatedHeaderFieldValues() {
        final RequestHeader header = RequestHeaderParser.parse(
                "POST /submit?x=1 HTTP/1.1\r\n"
                        + "Host: example.test\r\n"
                        + "Accept: text/plain \t\r\n"
                        + "accept:\tapplication/json\t\r\n"
                        + "X-Empty:\r\n"
                        + "X-Spaced: first\t second\r\n"
                        + "\r\n");

        assertAll(
                () -> assertEquals("POST", header.getMethod()),
                () -> assertEquals("/submit?x=1", header.getRequestTarget()),
                () -> assertEquals(1, header.getHttpMajorVersion()),
                () -> assertEquals(1, header.getHttpMinorVersion()),
                () -> assertEquals("example.test",
                        header.getFirstHeaderValue("HOST").orElseThrow()),
                () -> assertEquals(List.of("text/plain", "application/json"),
                        header.getHeaderValues("accept")),
                () -> assertEquals(List.of(""), header.getHeaderValues("x-empty")),
                () -> assertEquals(List.of("first\t second"),
                        header.getHeaderValues("x-spaced"))
        );
    }

    @Test
    void parseAcceptsRequestHeadWithoutAnyHeaderFields() {
        final RequestHeader header = RequestHeaderParser.parse("OPTIONS * HTTP/1.0\r\n\r\n");

        assertAll(
                () -> assertEquals("OPTIONS", header.getMethod()),
                () -> assertEquals("*", header.getRequestTarget()),
                () -> assertEquals(1, header.getHttpMajorVersion()),
                () -> assertEquals(0, header.getHttpMinorVersion()),
                () -> assertEquals(0, header.getHeaders().size())
        );
    }

    @Test
    void parseRejectsNullIncompleteBareLineEndingsAndTrailingBodyData() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> RequestHeaderParser.parse(null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("GET / HTTP/1.1")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("GET / HTTP/1.1\n\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("GET / HTTP/1.1\r\n\r\nbody")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("\r\n\r\n"))
        );
    }

    @Test
    void parseRejectsRequestLinesWithInvalidSpacingTokensTargetsOrVersions() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("GET  / HTTP/1.1\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("GET/ HTTP/1.1\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("GET / HTTP/1.1 extra\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("G(ET / HTTP/1.1\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("GET /not valid HTTP/1.1\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("GET / http/1.1\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse("GET / HTTP/1.10\r\n\r\n"))
        );
    }

    @Test
    void parseRejectsMissingNamesColonsWhitespaceBeforeColonsAndObsoleteFolding() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse(
                                "GET / HTTP/1.1\r\nHost example.test\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse(
                                "GET / HTTP/1.1\r\n: value\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse(
                                "GET / HTTP/1.1\r\nHost : example.test\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse(
                                "GET / HTTP/1.1\r\n folded value\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse(
                                "GET / HTTP/1.1\r\nX-Test: value\u0001\r\n\r\n")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeaderParser.parse(
                                "GET / HTTP/1.1\r\nX-Test: one\nOther: two\r\n\r\n"))
        );
    }
}
