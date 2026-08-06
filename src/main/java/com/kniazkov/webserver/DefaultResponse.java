/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an immutable HTTP response.
 */
final class DefaultResponse implements Response {

    /**
     * The HTTP response status.
     */
    private final HttpStatus status;

    /**
     * The response content type.
     */
    private final ContentType contentType;

    /**
     * The additional response header fields.
     */
    private final Map<String, List<String>> headers;

    /**
     * The response body data.
     */
    private final byte[] data;

    /**
     * Creates an immutable HTTP response.
     *
     * @param status
     *     the HTTP response status.
     * @param contentType
     *     the response content type.
     * @param headers
     *     the additional response header fields.
     * @param data
     *     the response body data.
     * @throws NullPointerException
     *     if any argument is {@code null}.
     */
    DefaultResponse(
        final HttpStatus status,
        final ContentType contentType,
        final Map<String, List<String>> headers,
        final byte[] data
    ) {
        this.status = Objects.requireNonNull(
            status,
            "HTTP status must not be null."
        );
        this.contentType = Objects.requireNonNull(
            contentType,
            "Content type must not be null."
        );
        this.headers = copyHeaders(
            Objects.requireNonNull(
                headers,
                "Response headers must not be null."
            )
        );

        final byte[] source = Objects.requireNonNull(
            data,
            "Response data must not be null."
        );
        this.data = Arrays.copyOf(source, source.length);
    }

    /**
     * Returns the HTTP response status.
     *
     * @return
     *     the response status.
     */
    @Override
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Returns the response content type.
     *
     * @return
     *     the response content type.
     */
    @Override
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * Returns the additional response header fields.
     *
     * @return
     *     an immutable map of response header fields.
     */
    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Returns a copy of the response body data.
     *
     * @return
     *     a copy of the response body data.
     */
    @Override
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Creates an immutable copy of the specified header map.
     *
     * @param source
     *     the source header map.
     * @return
     *     the immutable header map.
     */
    private static Map<String, List<String>> copyHeaders(
        final Map<String, List<String>> source
    ) {
        final Map<String, List<String>> copy = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            final String name = Objects.requireNonNull(
                entry.getKey(),
                "Header name must not be null."
            );
            final List<String> values = Objects.requireNonNull(
                entry.getValue(),
                "Header value list must not be null."
            );

            copy.put(name, List.copyOf(values));
        }

        return Collections.unmodifiableMap(copy);
    }
}
