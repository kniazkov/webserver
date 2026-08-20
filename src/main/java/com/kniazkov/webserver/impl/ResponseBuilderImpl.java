/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseBuilder;
import com.kniazkov.webserver.ResponseCookie;
import com.kniazkov.webserver.SameSite;
import com.kniazkov.webserver.ServerException;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    private final String contentType;

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
        ).getValue();
        this.data = data == null ? new byte[0] : data.clone();
    }

    /**
     * Creates a response builder with an arbitrary content type.
     *
     * @param status
     *     the HTTP status.
     * @param contentType
     *     the complete Content-Type value.
     * @param data
     *     the response body.
     */
    ResponseBuilderImpl(
        final HttpStatus status,
        final String contentType,
        final byte[] data
    ) {
        this.status = Objects.requireNonNull(
            status,
            "HTTP status must not be null"
        );
        this.contentType = Objects.requireNonNull(
            contentType,
            "Content type must not be null"
        ).trim();
        this.data = Objects.requireNonNull(
            data,
            "Response data must not be null"
        ).clone();
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

        validateCookie(value);
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
                values.add(formatCookie(cookie));
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

    /**
     * Validates a response cookie.
     *
     * @param name
     *     the cookie name.
     * @param value
     *     the cookie value.
     * @throws ServerException
     *     if the cookie is invalid.
     */
    private static void validateCookie(final ResponseCookie cookie)
        throws ServerException {

        final String name = cookie.getName();
        final String value = cookie.getValue();

        if (name == null || name.isEmpty()) {
            throw new ServerException(
                "Cookie name is missing"
            );
        }

        for (int index = 0; index < name.length(); index++) {
            if (!Lexer.isTokenCharacter(name.charAt(index))) {
                throw new ServerException(
                    "Invalid cookie name: " + name
                );
            }
        }

        if (value == null) {
            throw new ServerException(
                "Cookie value is missing"
            );
        }

        if (
            value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf(';') >= 0
        ) {
            throw new ServerException(
                "Invalid cookie value"
            );
        }

        if (cookie.getPath().isPresent()) {
            validateCookieAttribute(
                "path",
                cookie.getPath().get()
            );
        }

        if (cookie.getDomain().isPresent()) {
            validateCookieAttribute(
                "domain",
                cookie.getDomain().get()
            );
        }

        if (
            cookie.getSameSite().orElse(null) == SameSite.NONE
                && !cookie.isSecure()
        ) {
            throw new ServerException(
                "SameSite=None cookie must be secure"
            );
        }
    }

    /**
     * Validates an arbitrary Content-Type value.
     *
     * @param value
     *     the value to validate.
     * @throws ServerException
     *     if the value is invalid.
     */
    static void validateContentType(final String value)
        throws ServerException {

        if (value == null || value.isBlank()) {
            throw new ServerException("Content type is missing");
        }

        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);

            if (
                character == '\r'
                    || character == '\n'
                    || character == '\0'
            ) {
                throw new ServerException("Invalid content type");
            }
        }
    }

    /**
     * Validates a cookie attribute value.
     *
     * @param name
     *     the attribute name.
     * @param value
     *     the attribute value.
     * @throws ServerException
     *     if the value is invalid.
     */
    private static void validateCookieAttribute(
        final String name,
        final String value
    ) throws ServerException {

        if (value.isEmpty()) {
            throw new ServerException(
                "Cookie " + name + " is empty"
            );
        }

        if (
            value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf(';') >= 0
        ) {
            throw new ServerException(
                "Invalid cookie " + name
            );
        }
    }

    /**
     * Formats a Set-Cookie header value.
     *
     * @param cookie
     *     the response cookie.
     * @return
     *     the header value.
     */
    private static String formatCookie(final ResponseCookie cookie) {
        final StringBuilder result = new StringBuilder()
            .append(cookie.getName())
            .append('=')
            .append(cookie.getValue());

        cookie.getPath().ifPresent(
            value -> result.append("; Path=").append(value)
        );

        cookie.getDomain().ifPresent(
            value -> result.append("; Domain=").append(value)
        );

        cookie.getExpires().ifPresent(
            value -> result
                .append("; Expires=")
                .append(
                    DateTimeFormatter.RFC_1123_DATE_TIME.format(
                        value.atZone(ZoneOffset.UTC)
                    )
                )
        );

        cookie.getMaxAge().ifPresent(
            value -> result.append("; Max-Age=").append(value)
        );

        if (cookie.isSecure()) {
            result.append("; Secure");
        }

        if (cookie.isHttpOnly()) {
            result.append("; HttpOnly");
        }

        cookie.getSameSite().ifPresent(
            value -> result
                .append("; SameSite=")
                .append(value.getValue())
        );

        return result.toString();
    }
}
