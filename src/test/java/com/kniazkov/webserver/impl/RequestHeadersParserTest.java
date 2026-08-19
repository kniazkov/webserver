/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests parsing valid HTTP request headers.
 */
final class RequestHeadersParserTest extends RequestHeadersParserBaseTest {

    /**
     * Tests a request without header fields.
     */
    @Test
    void requestWithoutHeaders() throws ServerException {
        final RequestHeaders headers = parse(
            "GET / HTTP/1.1\r\n"
                + "\r\n"
        );

        assertEquals(HttpMethod.GET, headers.getMethod());
        assertEquals("/", headers.getTarget());
        assertEquals(HttpVersion.HTTP_1_1, headers.getVersion());
        assertEquals(Map.of(), headers.getValues());
    }

    /**
     * Tests parsing a simple GET request.
     */
    @Test
    void simpleGetRequest() throws ServerException {
        final RequestHeaders headers = parse(
            "GET /index.html HTTP/1.1\r\n"
                + "Host: example.com\r\n"
                + "Accept: text/html\r\n"
                + "\r\n"
        );

        assertEquals(HttpMethod.GET, headers.getMethod());
        assertEquals("/index.html", headers.getTarget());
        assertEquals(HttpVersion.HTTP_1_1, headers.getVersion());

        assertEquals(
            Map.of(
                "Host", List.of("example.com"),
                "Accept", List.of("text/html")
            ),
            headers.getValues()
        );
    }

    /**
     * Tests parsing a POST request.
     */
    @Test
    void postRequest() throws ServerException {
        final RequestHeaders headers = parse(
            "POST /submit HTTP/1.1\r\n"
                + "Content-Type: application/json\r\n"
                + "Content-Length: 17\r\n"
                + "\r\n"
        );

        assertEquals(HttpMethod.POST, headers.getMethod());
        assertEquals("/submit", headers.getTarget());
        assertEquals(HttpVersion.HTTP_1_1, headers.getVersion());

        assertEquals(
            Map.of(
                "Content-Type", List.of("application/json"),
                "Content-Length", List.of("17")
            ),
            headers.getValues()
        );
    }

    /**
     * Tests HTTP/1.0 request parsing.
     */
    @Test
    void http10Request() throws ServerException {
        final RequestHeaders headers = parse(
            "GET /legacy HTTP/1.0\r\n"
                + "Connection: close\r\n"
                + "\r\n"
        );

        assertEquals(HttpMethod.GET, headers.getMethod());
        assertEquals("/legacy", headers.getTarget());
        assertEquals(HttpVersion.HTTP_1_0, headers.getVersion());

        assertEquals(
            Map.of(
                "Connection", List.of("close")
            ),
            headers.getValues()
        );
    }

    /**
     * Tests canonicalization of header names.
     */
    @Test
    void canonicalHeaderNames() throws ServerException {
        final RequestHeaders headers = parse(
            "GET / HTTP/1.1\r\n"
                + "host: example.com\r\n"
                + "CONTENT-TYPE: text/plain\r\n"
                + "uSeR-aGeNt: Test\r\n"
                + "\r\n"
        );

        assertEquals(
            Map.of(
                "Host", List.of("example.com"),
                "Content-Type", List.of("text/plain"),
                "User-Agent", List.of("Test")
            ),
            headers.getValues()
        );
    }

    /**
     * Tests repeated header fields with different letter case.
     */
    @Test
    void repeatedHeaders() throws ServerException {
        final RequestHeaders headers = parse(
            "GET / HTTP/1.1\r\n"
                + "Accept: text/plain\r\n"
                + "accept: text/html\r\n"
                + "ACCEPT: application/json\r\n"
                + "\r\n"
        );

        assertEquals(
            Map.of(
                "Accept",
                List.of(
                    "text/plain",
                    "text/html",
                    "application/json"
                )
            ),
            headers.getValues()
        );
    }

    /**
     * Tests optional whitespace around a header value.
     */
    @Test
    void headerValueWhitespace() throws ServerException {
        final RequestHeaders headers = parse(
            "GET / HTTP/1.1\r\n"
                + "Accept:   text/plain\t \r\n"
                + "X-Empty:     \r\n"
                + "\r\n"
        );

        assertEquals(
            Map.of(
                "Accept", List.of("text/plain"),
                "X-Empty", List.of("")
            ),
            headers.getValues()
        );
    }

    /**
     * Tests a header value containing colons.
     */
    @Test
    void colonInsideHeaderValue() throws ServerException {
        final RequestHeaders headers = parse(
            "GET / HTTP/1.1\r\n"
                + "Location: http://localhost:8080/path\r\n"
                + "\r\n"
        );

        assertEquals(
            List.of("http://localhost:8080/path"),
            headers.getValues().get("Location")
        );
    }

    /**
     * Tests a request target containing a query string.
     */
    @Test
    void targetWithQuery() throws ServerException {
        final RequestHeaders headers = parse(
            "GET /search?q=test&limit=10 HTTP/1.1\r\n"
                + "\r\n"
        );

        assertEquals(
            "/search?q=test&limit=10",
            headers.getTarget()
        );
    }

    /**
     * Tests a request with many header fields.
     */
    @Test
    void manyHeaders() throws ServerException {
        final RequestHeaders headers = parse(
            "GET /resource HTTP/1.1\r\n"
                + "Host: example.com\r\n"
                + "User-Agent: TestClient\r\n"
                + "Accept: text/html\r\n"
                + "Accept-Encoding: gzip\r\n"
                + "Cache-Control: no-cache\r\n"
                + "\r\n"
        );

        assertEquals(
            Map.of(
                "Host", List.of("example.com"),
                "User-Agent", List.of("TestClient"),
                "Accept", List.of("text/html"),
                "Accept-Encoding", List.of("gzip"),
                "Cache-Control", List.of("no-cache")
            ),
            headers.getValues()
        );
    }
}
