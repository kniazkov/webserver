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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link CookieParser}.
 */
final class CookieParserTest {

    /**
     * Tests parsing a simple cookie header.
     */
    @Test
    void simpleCookies() throws ServerException {
        final Request request = parse(
            "session=abc123; theme=dark; language=ru"
        );

        assertEquals(
            Map.of(
                "session", "abc123",
                "theme", "dark",
                "language", "ru"
            ),
            request.getCookies()
        );
    }

    /**
     * Tests a request without cookies.
     */
    @Test
    void noCookies() throws ServerException {
        final RequestBuilder builder = new RequestBuilder()
            .setHeaders(headers())
            .setPath(RootRequestPath.getInstance());

        CookieParser.parse(headers(), builder);

        final Request request = builder.build();

        assertTrue(request.getCookies().isEmpty());
    }

    /**
     * Tests a cookie with an empty value.
     */
    @Test
    void emptyValue() throws ServerException {
        final Request request = parse(
            "session=; theme=dark"
        );

        assertEquals(
            Map.of(
                "session", "",
                "theme", "dark"
            ),
            request.getCookies()
        );
    }

    /**
     * Tests cookie values containing equals signs.
     */
    @Test
    void equalsInsideValue() throws ServerException {
        final Request request = parse(
            "token=abc=def==; mode=test"
        );

        assertEquals(
            "abc=def==",
            request.getCookies().get("token")
        );
    }

    /**
     * Tests optional whitespace around cookies.
     */
    @Test
    void whitespace() throws ServerException {
        final Request request = parse(
            " session = abc123 ; theme = dark "
        );

        assertEquals(
            Map.of(
                "session", "abc123",
                "theme", "dark"
            ),
            request.getCookies()
        );
    }

    /**
     * Tests several Cookie header fields.
     */
    @Test
    void severalCookieHeaders() throws ServerException {
        final RequestHeaders headers = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Cookie", "one=1; two=2")
            .addValue("cookie", "three=3")
            .build();

        final RequestBuilder builder = new RequestBuilder()
            .setHeaders(headers)
            .setPath(RootRequestPath.getInstance());

        CookieParser.parse(headers, builder);

        final Request request = builder.build();

        assertEquals(
            Map.of(
                "one", "1",
                "two", "2",
                "three", "3"
            ),
            request.getCookies()
        );
    }

    /**
     * Tests empty segments between separators.
     */
    @Test
    void emptySegments() throws ServerException {
        final Request request = parse(
            "one=1;; ;two=2;"
        );

        assertEquals(
            Map.of(
                "one", "1",
                "two", "2"
            ),
            request.getCookies()
        );
    }

    /**
     * Tests duplicate cookie names.
     */
    @Test
    void duplicateName() throws ServerException {
        final Request request = parse(
            "value=first; value=second"
        );

        assertEquals(
            "second",
            request.getCookies().get("value")
        );
    }

    /**
     * Tests a cookie without an equals sign.
     */
    @Test
    void missingEquals() {
        assertThrows(
            ServerException.class,
            () -> parse("session")
        );
    }

    /**
     * Tests a cookie with an empty name.
     */
    @Test
    void emptyName() {
        assertThrows(
            ServerException.class,
            () -> parse("=abc123")
        );
    }

    /**
     * Tests whitespace-only cookie name.
     */
    @Test
    void blankName() {
        assertThrows(
            ServerException.class,
            () -> parse("   =abc123")
        );
    }

    /**
     * Parses cookies into a request.
     *
     * @param value
     *     the Cookie header value.
     * @return
     *     the resulting request.
     * @throws ServerException
     *     if parsing fails.
     */
    private static Request parse(final String value)
        throws ServerException {
        final RequestHeaders headers = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Cookie", value)
            .build();

        final RequestBuilder builder = new RequestBuilder()
            .setHeaders(headers)
            .setPath(RootRequestPath.getInstance());

        CookieParser.parse(headers, builder);

        return builder.build();
    }

    /**
     * Creates request headers without cookies.
     *
     * @return
     *     the request headers.
     * @throws ServerException
     *     if building fails.
     */
    private static RequestHeaders headers() throws ServerException {
        return new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .build();
    }
}
