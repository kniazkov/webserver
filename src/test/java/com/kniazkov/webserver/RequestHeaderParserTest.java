/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link RequestHeaderParser} class.
 */
final class RequestHeaderParserTest {

    /**
     * Tests parsing a complete HTTP request header.
     *
     * @throws ServerException
     *     if parsing or building fails.
     */
    @Test
    void parsesCompleteRequestHeader() throws ServerException {
        final RequestHeader.Builder builder = RequestHeader.builder();
        final RequestHeaderParser parser = new RequestHeaderParser(builder);

        parser.parseLine("GET /index.html?name=Ivan HTTP/1.1");
        parser.parseLine("Host: example.com");
        parser.parseLine("Accept: text/html");
        parser.parseLine("");

        final RequestHeader header = builder.build();

        assertEquals(HttpMethod.GET, header.getMethod());
        assertEquals("/index.html?name=Ivan", header.getTarget());
        assertEquals(HttpVersion.HTTP_1_1, header.getVersion());
        assertEquals(
            Map.of(
                "Host", List.of("example.com"),
                "Accept", List.of("text/html")
            ),
            header.getValues()
        );
        assertTrue(parser.isFinished());
    }

    /**
     * Tests parsing a POST request using HTTP/1.0.
     *
     * @throws ServerException
     *     if parsing or building fails.
     */
    @Test
    void parsesPostRequest() throws ServerException {
        final RequestHeader.Builder builder = RequestHeader.builder();
        final RequestHeaderParser parser = new RequestHeaderParser(builder);

        parser.parseLine("POST /submit HTTP/1.0");
        parser.parseLine("");

        final RequestHeader header = builder.build();

        assertEquals(HttpMethod.POST, header.getMethod());
        assertEquals("/submit", header.getTarget());
        assertEquals(HttpVersion.HTTP_1_0, header.getVersion());
        assertEquals(Map.of(), header.getValues());
    }

    /**
     * Tests that repeated header fields are preserved.
     *
     * @throws ServerException
     *     if parsing or building fails.
     */
    @Test
    void parsesRepeatedHeaderFields() throws ServerException {
        final RequestHeader.Builder builder = RequestHeader.builder();
        final RequestHeaderParser parser = new RequestHeaderParser(builder);

        parser.parseLine("GET / HTTP/1.1");
        parser.parseLine("Accept: text/html");
        parser.parseLine("Accept: application/json");
        parser.parseLine("");

        final RequestHeader header = builder.build();

        assertEquals(
            List.of("text/html", "application/json"),
            header.getValues().get("Accept")
        );
    }

    /**
     * Tests that whitespace around a header value is removed.
     *
     * @throws ServerException
     *     if parsing or building fails.
     */
    @Test
    void trimsHeaderValue() throws ServerException {
        final RequestHeader.Builder builder = RequestHeader.builder();
        final RequestHeaderParser parser = new RequestHeaderParser(builder);

        parser.parseLine("GET / HTTP/1.1");
        parser.parseLine("Host:   example.com   ");
        parser.parseLine("");

        final RequestHeader header = builder.build();

        assertEquals(
            List.of("example.com"),
            header.getValues().get("Host")
        );
    }

    /**
     * Tests parsing a header field with an empty value.
     *
     * @throws ServerException
     *     if parsing or building fails.
     */
    @Test
    void parsesEmptyHeaderValue() throws ServerException {
        final RequestHeader.Builder builder = RequestHeader.builder();
        final RequestHeaderParser parser = new RequestHeaderParser(builder);

        parser.parseLine("GET / HTTP/1.1");
        parser.parseLine("X-Empty:");
        parser.parseLine("");

        final RequestHeader header = builder.build();

        assertEquals(
            List.of(""),
            header.getValues().get("X-Empty")
        );
    }

    /**
     * Tests that colons inside a header value are preserved.
     *
     * @throws ServerException
     *     if parsing or building fails.
     */
    @Test
    void preservesColonInsideHeaderValue() throws ServerException {
        final RequestHeader.Builder builder = RequestHeader.builder();
        final RequestHeaderParser parser = new RequestHeaderParser(builder);

        parser.parseLine("GET / HTTP/1.1");
        parser.parseLine("Location: http://example.com:8080/page");
        parser.parseLine("");

        final RequestHeader header = builder.build();

        assertEquals(
            List.of("http://example.com:8080/page"),
            header.getValues().get("Location")
        );
    }

    /**
     * Tests that the parser is not initially finished.
     */
    @Test
    void isNotInitiallyFinished() {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        assertFalse(parser.isFinished());
    }

    /**
     * Tests that the parser is not finished before the terminating line.
     *
     * @throws ServerException
     *     if parsing fails.
     */
    @Test
    void isNotFinishedBeforeEmptyLine() throws ServerException {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        parser.parseLine("GET / HTTP/1.1");
        parser.parseLine("Host: example.com");

        assertFalse(parser.isFinished());
    }

    /**
     * Tests that a null builder is rejected.
     */
    @Test
    void rejectsNullBuilder() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new RequestHeaderParser(null)
        );

        assertEquals(
            "Builder must not be null.",
            exception.getMessage()
        );
    }

    /**
     * Tests that a null line is rejected.
     */
    @Test
    void rejectsNullLine() {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine(null)
        );

        assertEquals(
            "HTTP header line must not be null.",
            exception.getMessage()
        );
    }

    /**
     * Tests that CR characters inside a line are rejected.
     */
    @Test
    void rejectsCarriageReturn() {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine("GET / HTTP/1.1\r")
        );

        assertEquals(
            "HTTP header line must not contain CR or LF characters.",
            exception.getMessage()
        );
    }

    /**
     * Tests that LF characters inside a line are rejected.
     */
    @Test
    void rejectsLineFeed() {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine("GET / HTTP/1.1\n")
        );

        assertEquals(
            "HTTP header line must not contain CR or LF characters.",
            exception.getMessage()
        );
    }

    /**
     * Tests that an empty request line is rejected.
     */
    @Test
    void rejectsEmptyRequestLine() {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine("")
        );

        assertEquals(
            "Request line must not be empty.",
            exception.getMessage()
        );
    }

    /**
     * Tests that a request line with too few parts is rejected.
     */
    @Test
    void rejectsIncompleteRequestLine() {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine("GET /")
        );

        assertEquals(
            "Malformed HTTP request line: GET /",
            exception.getMessage()
        );
    }

    /**
     * Tests that a request line with extra spaces is rejected.
     */
    @Test
    void rejectsRequestLineWithExtraSpaces() {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine("GET  / HTTP/1.1")
        );

        assertEquals(
            "Malformed HTTP request line: GET  / HTTP/1.1",
            exception.getMessage()
        );
    }

    /**
     * Tests that an unsupported HTTP method is rejected.
     */
    @Test
    void rejectsUnsupportedMethod() {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine("DELETE / HTTP/1.1")
        );

        assertEquals(
            "Unsupported HTTP method: DELETE",
            exception.getMessage()
        );
    }

    /**
     * Tests that an unsupported HTTP version is rejected.
     */
    @Test
    void rejectsUnsupportedVersion() {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine("GET / HTTP/2")
        );

        assertEquals(
            "Unsupported HTTP version: HTTP/2",
            exception.getMessage()
        );
    }

    /**
     * Tests that a header field without a colon is rejected.
     *
     * @throws ServerException
     *     if parsing the request line fails.
     */
    @Test
    void rejectsHeaderWithoutColon() throws ServerException {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        parser.parseLine("GET / HTTP/1.1");

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine("Host example.com")
        );

        assertEquals(
            "Header field does not contain a colon: Host example.com",
            exception.getMessage()
        );
    }

    /**
     * Tests that a header field with an empty name is rejected.
     *
     * @throws ServerException
     *     if parsing the request line fails.
     */
    @Test
    void rejectsEmptyHeaderName() throws ServerException {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        parser.parseLine("GET / HTTP/1.1");

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine(": example.com")
        );

        assertEquals(
            "Header field name must not be empty.",
            exception.getMessage()
        );
    }

    /**
     * Tests that obsolete continuation lines are rejected.
     *
     * @throws ServerException
     *     if parsing the request line fails.
     */
    @Test
    void rejectsHeaderContinuationLine() throws ServerException {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        parser.parseLine("GET / HTTP/1.1");

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine(" continuation")
        );

        assertEquals(
            "Header continuation lines are not supported:  continuation",
            exception.getMessage()
        );
    }

    /**
     * Tests that additional lines after the header are rejected.
     *
     * @throws ServerException
     *     if parsing the valid header fails.
     */
    @Test
    void rejectsDataAfterHeaderEnd() throws ServerException {
        final RequestHeaderParser parser =
            new RequestHeaderParser(RequestHeader.builder());

        parser.parseLine("GET / HTTP/1.1");
        parser.parseLine("");

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.parseLine("Host: example.com")
        );

        assertEquals(
            "Unexpected data after the end of the request header.",
            exception.getMessage()
        );
    }
}
