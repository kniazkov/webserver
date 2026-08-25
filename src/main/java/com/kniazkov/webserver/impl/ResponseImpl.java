/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Response;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default immutable implementation of {@link Response}.
 */
final class ResponseImpl implements Response {

    /**
     * The HTTP status.
     */
    private final HttpStatus status;

    /**
     * The response media type.
     */
    private final ContentType contentType;

    /**
     * The optional character encoding.
     */
    private final Charset charset;

    /**
     * The immutable response headers.
     */
    private final Map<String, List<String>> headers;

    /**
     * The response body.
     */
    private final byte[] data;

    /**
     * Creates an immutable response without an explicit character encoding.
     *
     * @param status
     *     the HTTP status.
     * @param contentType
     *     the response media type.
     * @param headers
     *     the response headers.
     * @param data
     *     the response body.
     */
    ResponseImpl(
        final HttpStatus status,
        final ContentType contentType,
        final Map<String, List<String>> headers,
        final byte[] data
    ) {
        this(status, contentType, headers, data, null);
    }

    /**
     * Creates an immutable response.
     *
     * @param status
     *     the HTTP status.
     * @param contentType
     *     the response media type.
     * @param headers
     *     the response headers.
     * @param data
     *     the response body.
     * @param charset
     *     the optional character encoding.
     */
    ResponseImpl(
        final HttpStatus status,
        final ContentType contentType,
        final Map<String, List<String>> headers,
        final byte[] data,
        final Charset charset
    ) {
        this.status = status;
        this.contentType = contentType;
        this.charset = charset;

        final Map<String, List<String>> copy =
            new LinkedHashMap<>();

        headers.forEach(
            (name, values) -> copy.put(
                name,
                List.copyOf(values)
            )
        );

        this.headers = Map.copyOf(copy);
        this.data = data.clone();
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public ContentType getContentType() {
        return contentType;
    }

    @Override
    public Optional<Charset> getCharset() {
        return Optional.ofNullable(charset);
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public byte[] getData() {
        return data.clone();
    }
}
