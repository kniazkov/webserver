/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseBuilder;
import com.kniazkov.webserver.ResponseCookie;
import com.kniazkov.webserver.ServerException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link ResponseBuilder}.
 */
final class ResponseBuilderImpl implements ResponseBuilder {

    /**
     * The HTTP status.
     */
    private final HttpStatus status;

    /**
     * The content type.
     */
    private final ContentType contentType;

    /**
     * The response data.
     */
    private final byte[] data;

    /**
     * The response headers.
     */
    private final Map<String, List<String>> headers =
        new LinkedHashMap<>();

    /**
     * The response cookies.
     */
    private final Map<String, ResponseCookie> cookies =
        new LinkedHashMap<>();

    /**
     * Creates a response builder without a body.
     *
     * @param status
     *     the HTTP status.
     * @param contentType
     *     the content type.
     */
    ResponseBuilderImpl(
        final HttpStatus status,
        final ContentType contentType
    ) {
        this(status, contentType, null);
    }

    /**
     * Creates a response builder.
     *
     * @param status
     *     the HTTP status.
     * @param contentType
     *     the content type.
     * @param data
     *     the response data, or {@code null} if there is no body.
     */
    ResponseBuilderImpl(
        final HttpStatus status,
        final ContentType contentType,
        final byte[] data
    ) {
        this.status = Objects.requireNonNull(
            status,
            "HTTP status must not be null"
        );
        this.contentType = Objects.requireNonNull(
            contentType,
            "Content type must not be null"
        );
        this.data = data == null ? new byte[0] : data.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder addHeader(
        final String name,
        final String value
    ) throws ServerException {
        final String canonical = validateHeader(name, value);

        headers.computeIfAbsent(
            canonical,
            ignored -> new ArrayList<>()
        ).add(value);

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder setHeader(
        final String name,
        final String value
    ) throws ServerException {
        final String canonical = validateHeader(name, value);

        headers.put(
            canonical,
            new ArrayList<>(List.of(value))
        );

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder setCookie(
        final String name,
        final String value
    ) throws ServerException {
        if (name == null) {
            throw new ServerException("Cookie name is missing");
        }

        if (value == null) {
            throw new ServerException("Cookie value is missing");
        }

        return setCookie(
            new ResponseCookie.Builder(name, value).build()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder setCookie(final ResponseCookie cookie)
        throws ServerException {

        final ResponseCookie value = Objects.requireNonNull(
            cookie,
            "Cookie must not be null"
        );

        cookies.put(value.getName(), value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response build() throws ServerException {
        if (!status.allowsBody() && data.length != 0) {
            throw new ServerException(
                "HTTP status " + status + " does not permit a body"
            );
        }

        final Map<String, List<String>> result =
            new LinkedHashMap<>();

        for (
            Map.Entry<String, List<String>> entry
            : headers.entrySet()
        ) {
            result.put(
                entry.getKey(),
                List.copyOf(entry.getValue())
            );
        }

        if (!cookies.isEmpty()) {
            final List<String> values = new ArrayList<>();

            for (ResponseCookie cookie : cookies.values()) {
                values.add(cookie.toString());
            }

            result.computeIfAbsent(
                "Set-Cookie",
                ignored -> new ArrayList<>()
            ).addAll(values);
        }

        return new ResponseImpl(
            status,
            contentType,
            result,
            data
        );
    }

    /**
     * Validates a response header and returns its canonical name.
     *
     * @param name
     *     the header name.
     * @param value
     *     the header value.
     * @return
     *     the canonical header name.
     * @throws ServerException
     *     if the header is invalid.
     */
    private static String validateHeader(
        final String name,
        final String value
    ) throws ServerException {
        if (name == null || name.isEmpty()) {
            throw new ServerException(
                "Response header name is missing"
            );
        }

        if (value == null) {
            throw new ServerException(
                "Response header value is missing"
            );
        }

        for (int index = 0; index < name.length(); index++) {
            if (!Lexer.isTokenCharacter(name.charAt(index))) {
                throw new ServerException(
                    "Invalid response header name: " + name
                );
            }
        }

        if (
            name.equalsIgnoreCase("Content-Type")
                || name.equalsIgnoreCase("Content-Length")
                || name.equalsIgnoreCase("Transfer-Encoding")
        ) {
            throw new ServerException(
                "Response header is managed by the server: " + name
            );
        }

        if (
            value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0
        ) {
            throw new ServerException(
                "Invalid response header value"
            );
        }

        return Lexer.canonicalizeHeaderName(name);
    }

}
