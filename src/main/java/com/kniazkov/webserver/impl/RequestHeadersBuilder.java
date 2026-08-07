/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.ServerException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds HTTP request headers.
 */
public final class RequestHeadersBuilder {

    /**
     * The HTTP method.
     */
    private HttpMethod method;

    /**
     * The request target.
     */
    private String target;

    /**
     * The HTTP protocol version.
     */
    private HttpVersion version;

    /**
     * The HTTP header values.
     */
    private final Map<String, List<String>> values = new LinkedHashMap<>();

    /**
     * Sets the HTTP method.
     *
     * @param value
     *     the HTTP method.
     * @return
     *     this builder.
     */
    public RequestHeadersBuilder setMethod(final HttpMethod value) {
        method = value;
        return this;
    }

    /**
     * Sets the request target.
     *
     * @param value
     *     the request target.
     * @return
     *     this builder.
     */
    public RequestHeadersBuilder setTarget(final String value) {
        target = value;
        return this;
    }

    /**
     * Sets the HTTP protocol version.
     *
     * @param value
     *     the HTTP protocol version.
     * @return
     *     this builder.
     */
    public RequestHeadersBuilder setVersion(final HttpVersion value) {
        version = value;
        return this;
    }

    /**
     * Adds a header value.
     *
     * @param name
     *     the header name.
     * @param value
     *     the header value.
     * @return
     *     this builder.
     * @throws ServerException
     *     if the header name or value is invalid.
     */
    public RequestHeadersBuilder addValue(
        final String name,
        final String value
    ) throws ServerException {
        validateName(name);
        validateValue(value);
        values.computeIfAbsent(
            Lexer.canonicalizeHeaderName(name),
            key -> new ArrayList<>()
        ).add(value);
        return this;
    }

    /**
     * Replaces all values of a header with the specified value.
     *
     * @param name
     *     the header name.
     * @param value
     *     the header value.
     * @return
     *     this builder.
     * @throws ServerException
     *     if the header name or value is invalid.
     */
    public RequestHeadersBuilder setValue(
        final String name,
        final String value
    ) throws ServerException {
        validateName(name);
        validateValue(value);
        values.put(
            Lexer.canonicalizeHeaderName(name),
            new ArrayList<>(List.of(value))
        );
        return this;
    }

    /**
     * Builds immutable HTTP request headers.
     *
     * @return
     *     the HTTP request headers.
     * @throws ServerException
     *     if the request headers are incomplete.
     */
    public RequestHeaders build() throws ServerException {
        if (method == null) {
            throw new ServerException("HTTP method is not specified");
        }
        if (target == null || target.isBlank()) {
            throw new ServerException("Request target is not specified");
        }
        if (version == null) {
            throw new ServerException("HTTP version is not specified");
        }

        return new RequestHeadersImpl(method, target, version, values);
    }

    /**
     * Validates a header name.
     *
     * @param name
     *     the header name.
     * @throws ServerException
     *     if the header name is invalid.
     */
    private static void validateName(final String name) throws ServerException {
        if (name == null || name.isEmpty()) {
            throw new ServerException("HTTP header name is empty");
        }

        for (int index = 0; index < name.length(); index++) {
            final char ch = name.charAt(index);
            if (!Lexer.isTokenCharacter(ch)) {
                throw new ServerException("Invalid HTTP header name: " + name);
            }
        }
    }

    /**
     * Validates a header value.
     *
     * @param value
     *     the header value.
     * @throws ServerException
     *     if the header value is invalid.
     */
    private static void validateValue(final String value) throws ServerException {
        if (value == null) {
            throw new ServerException("HTTP header value is null");
        }

        for (int index = 0; index < value.length(); index++) {
            final char ch = value.charAt(index);
            if (ch == Lexer.CR || ch == Lexer.LF || ch == Lexer.NUL) {
                throw new ServerException("Invalid HTTP header value");
            }
        }
    }
}
