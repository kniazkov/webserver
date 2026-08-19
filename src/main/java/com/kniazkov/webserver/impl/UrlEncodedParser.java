/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

import java.nio.charset.StandardCharsets;

/**
 * Parses URL-encoded request parameters.
 */
final class UrlEncodedParser {

    /**
     * Prevents instantiation.
     */
    private UrlEncodedParser() {
    }

    /**
     * Parses query parameters from a request target.
     *
     * @param target
     *     the request target.
     * @param builder
     *     the request builder.
     * @throws ServerException
     *     if the query string is invalid.
     */
    static void parseQuery(
        final String target,
        final RequestBuilder builder
    ) throws ServerException {
        final int question = target.indexOf('?');

        if (question < 0 || question == target.length() - 1) {
            return;
        }

        parse(
            target.substring(question + 1),
            builder,
            true
        );
    }

    /**
     * Parses URL-encoded form data.
     *
     * @param data
     *     the form data.
     * @param builder
     *     the request builder.
     * @throws ServerException
     *     if the form data is invalid.
     */
    static void parseForm(
        final byte[] data,
        final RequestBuilder builder
    ) throws ServerException {
        if (data.length == 0) {
            return;
        }

        parse(
            new String(data, StandardCharsets.US_ASCII),
            builder,
            false
        );
    }

    /**
     * Parses URL-encoded parameters.
     *
     * @param value
     *     the encoded parameters.
     * @param builder
     *     the request builder.
     * @param query
     *     whether parameters belong to the query string.
     * @throws ServerException
     *     if the encoded data is invalid.
     */
    private static void parse(
        final String value,
        final RequestBuilder builder,
        final boolean query
    ) throws ServerException {
        int start = 0;

        while (start <= value.length()) {
            final int ampersand = value.indexOf('&', start);
            final int end = ampersand < 0
                ? value.length()
                : ampersand;

            if (end > start) {
                parseParameter(
                    value.substring(start, end),
                    builder,
                    query
                );
            }

            if (ampersand < 0) {
                return;
            }

            start = ampersand + 1;
        }
    }

    /**
     * Parses one URL-encoded parameter.
     *
     * @param value
     *     the encoded parameter.
     * @param builder
     *     the request builder.
     * @param query
     *     whether the parameter belongs to the query string.
     * @throws ServerException
     *     if the parameter is invalid.
     */
    private static void parseParameter(
        final String value,
        final RequestBuilder builder,
        final boolean query
    ) throws ServerException {
        final int equals = value.indexOf('=');

        final String name;
        final String parameterValue;

        if (equals < 0) {
            name = decode(value);
            parameterValue = "";
        } else {
            name = decode(value.substring(0, equals));
            parameterValue = decode(value.substring(equals + 1));
        }

        if (name.isEmpty()) {
            throw new ServerException(
                "URL-encoded parameter name is empty"
            );
        }

        if (query) {
            builder.addQuery(name, parameterValue);
        } else {
            builder.addForm(name, parameterValue);
        }
    }

    /**
     * Decodes a URL-encoded string using UTF-8.
     *
     * @param value
     *     the encoded value.
     * @return
     *     the decoded value.
     * @throws ServerException
     *     if percent encoding is invalid.
     */
    private static String decode(final String value)
        throws ServerException {
        final byte[] data = new byte[value.length()];
        int size = 0;

        for (int index = 0; index < value.length(); index++) {
            final char ch = value.charAt(index);

            if (ch == '+') {
                data[size++] = (byte) ' ';
            } else if (ch == '%') {
                if (index + 2 >= value.length()) {
                    throw new ServerException(
                        "Invalid URL encoding"
                    );
                }

                final int high = hex(value.charAt(++index));
                final int low = hex(value.charAt(++index));

                if (high < 0 || low < 0) {
                    throw new ServerException(
                        "Invalid URL encoding"
                    );
                }

                data[size++] = (byte) ((high << 4) | low);
            } else {
                if (ch > 0x7f) {
                    throw new ServerException(
                        "Invalid URL encoding"
                    );
                }

                data[size++] = (byte) ch;
            }
        }

        return new String(
            data,
            0,
            size,
            StandardCharsets.UTF_8
        );
    }

    /**
     * Converts a hexadecimal character to its numeric value.
     *
     * @param ch
     *     the character.
     * @return
     *     the numeric value, or {@code -1} if the character is invalid.
     */
    private static int hex(final char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        return -1;
    }
}
