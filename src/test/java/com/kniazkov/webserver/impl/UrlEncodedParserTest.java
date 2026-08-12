/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link UrlEncodedParser}.
 */
final class UrlEncodedParserTest {

    /**
     * Tests parsing a simple query string.
     */
    @Test
    void simpleQuery() throws ServerException {
        final Request request = query(
            "/search?q=java&page=2"
        );

        assertEquals(
            Map.of(
                "q", List.of("java"),
                "page", List.of("2")
            ),
            request.getQuery()
        );
    }

    /**
     * Tests a target without a query string.
     */
    @Test
    void noQuery() throws ServerException {
        final Request request = query("/index.html");

        assertTrue(request.getQuery().isEmpty());
    }

    /**
     * Tests an empty query string.
     */
    @Test
    void emptyQuery() throws ServerException {
        final Request request = query("/?");

        assertTrue(request.getQuery().isEmpty());
    }

    /**
     * Tests repeated query parameters.
     */
    @Test
    void repeatedQueryParameters() throws ServerException {
        final Request request = query(
            "/search?tag=java&tag=http&tag=server"
        );

        assertEquals(
            List.of("java", "http", "server"),
            request.getQuery().get("tag")
        );
    }

    /**
     * Tests a parameter without an equals sign.
     */
    @Test
    void parameterWithoutValue() throws ServerException {
        final Request request = query(
            "/search?debug"
        );

        assertEquals(
            List.of(""),
            request.getQuery().get("debug")
        );
    }

    /**
     * Tests an explicitly empty parameter value.
     */
    @Test
    void emptyValue() throws ServerException {
        final Request request = query(
            "/search?q="
        );

        assertEquals(
            List.of(""),
            request.getQuery().get("q")
        );
    }

    /**
     * Tests decoding plus signs as spaces.
     */
    @Test
    void plusAsSpace() throws ServerException {
        final Request request = query(
            "/search?q=hello+world"
        );

        assertEquals(
            List.of("hello world"),
            request.getQuery().get("q")
        );
    }

    /**
     * Tests percent decoding.
     */
    @Test
    void percentEncoding() throws ServerException {
        final Request request = query(
            "/search?q=hello%20world%21"
        );

        assertEquals(
            List.of("hello world!"),
            request.getQuery().get("q")
        );
    }

    /**
     * Tests decoding UTF-8 data.
     */
    @Test
    void utf8() throws ServerException {
        final Request request = query(
            "/search?"
                + "name=%D0%98%D0%B2%D0%B0%D0%BD"
        );

        assertEquals(
            List.of("Иван"),
            request.getQuery().get("name")
        );
    }

    /**
     * Tests parsing URL-encoded form data.
     */
    @Test
    void simpleForm() throws ServerException {
        final Request request = form(
            "name=Ivan&city=Prague"
        );

        assertEquals(
            Map.of(
                "name", List.of("Ivan"),
                "city", List.of("Prague")
            ),
            request.getForm()
        );
    }

    /**
     * Tests parsing an empty form body.
     */
    @Test
    void emptyForm() throws ServerException {
        final Request request = form("");

        assertTrue(request.getForm().isEmpty());
    }

    /**
     * Tests repeated form parameters.
     */
    @Test
    void repeatedFormParameters() throws ServerException {
        final Request request = form(
            "value=one&value=two&value=three"
        );

        assertEquals(
            List.of("one", "two", "three"),
            request.getForm().get("value")
        );
    }

    /**
     * Tests decoding parameter names.
     */
    @Test
    void encodedName() throws ServerException {
        final Request request = form(
            "first%20name=Ivan"
        );

        assertEquals(
            List.of("Ivan"),
            request.getForm().get("first name")
        );
    }

    /**
     * Tests empty parameter names.
     */
    @Test
    void emptyName() {
        assertThrows(
            ServerException.class,
            () -> query("/search?=value")
        );
    }

    /**
     * Tests incomplete percent encoding.
     */
    @Test
    void incompletePercentEncoding() {
        assertThrows(
            ServerException.class,
            () -> query("/search?q=%")
        );

        assertThrows(
            ServerException.class,
            () -> query("/search?q=%A")
        );
    }

    /**
     * Tests invalid hexadecimal digits in percent encoding.
     */
    @Test
    void invalidPercentEncoding() {
        assertThrows(
            ServerException.class,
            () -> query("/search?q=%GG")
        );
    }

    /**
     * Tests empty segments between ampersands.
     */
    @Test
    void emptySegments() throws ServerException {
        final Request request = query(
            "/search?a=1&&b=2&"
        );

        assertEquals(
            Map.of(
                "a", List.of("1"),
                "b", List.of("2")
            ),
            request.getQuery()
        );
    }

    /**
     * Parses query parameters into a request.
     *
     * @param target
     *     the request target.
     * @return
     *     the request.
     * @throws ServerException
     *     if parsing fails.
     */
    private static Request query(final String target)
        throws ServerException {
        final RequestBuilder builder = new RequestBuilder()
            .setHeaders(headers(target));

        UrlEncodedParser.parseQuery(target, builder);

        return builder.build();
    }

    /**
     * Parses form parameters into a request.
     *
     * @param value
     *     the encoded form.
     * @return
     *     the request.
     * @throws ServerException
     *     if parsing fails.
     */
    private static Request form(final String value)
        throws ServerException {
        final RequestBuilder builder = new RequestBuilder()
            .setHeaders(headers("/"));

        UrlEncodedParser.parseForm(
            value.getBytes(StandardCharsets.US_ASCII),
            builder
        );

        return builder.build();
    }

    /**
     * Creates request headers for testing.
     *
     * @param target
     *     the request target.
     * @return
     *     the request headers.
     * @throws ServerException
     *     if building fails.
     */
    private static RequestHeaders headers(final String target)
        throws ServerException {
        return new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget(target)
            .setVersion(HttpVersion.HTTP_1_1)
            .build();
    }
}
