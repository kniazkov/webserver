/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.ServerException;

import java.util.List;

/**
 * Parses cookies from HTTP request headers.
 */
final class CookieParser {

    /**
     * Prevents instantiation.
     */
    private CookieParser() {
    }

    /**
     * Parses cookies from request headers and adds them to the request builder.
     *
     * @param headers
     *     the request headers.
     * @param builder
     *     the request builder.
     * @throws ServerException
     *     if a cookie header is invalid.
     */
    static void parse(
        final RequestHeaders headers,
        final RequestBuilder builder
    ) throws ServerException {
        final List<String> values = headers.getValues().get("Cookie");

        if (values == null) {
            return;
        }

        for (String value : values) {
            parse(value, builder);
        }
    }

    /**
     * Parses one Cookie header value.
     *
     * @param value
     *     the header value.
     * @param builder
     *     the request builder.
     * @throws ServerException
     *     if the cookie header is invalid.
     */
    private static void parse(
        final String value,
        final RequestBuilder builder
    ) throws ServerException {
        int start = 0;

        while (start < value.length()) {
            final int separator = value.indexOf(';', start);
            final int end = separator < 0
                ? value.length()
                : separator;

            final String cookie = value.substring(start, end).trim();

            if (!cookie.isEmpty()) {
                parseCookie(cookie, builder);
            }

            if (separator < 0) {
                return;
            }

            start = separator + 1;
        }
    }

    /**
     * Parses one cookie.
     *
     * @param value
     *     the cookie text.
     * @param builder
     *     the request builder.
     * @throws ServerException
     *     if the cookie is invalid.
     */
    private static void parseCookie(
        final String value,
        final RequestBuilder builder
    ) throws ServerException {
        final int equals = value.indexOf('=');

        if (equals <= 0) {
            throw new ServerException(
                "Invalid cookie: " + value
            );
        }

        final String name = value.substring(0, equals).trim();
        final String cookieValue = value.substring(equals + 1).trim();

        if (name.isEmpty()) {
            throw new ServerException(
                "Invalid cookie: " + value
            );
        }

        builder.setCookie(name, cookieValue);
    }
}
