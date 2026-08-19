/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default immutable implementation of {@link Response}.
 */
final class ResponseImpl implements Response {

    private final HttpStatus status;

    private final String contentType;

    private final Map<String, List<String>> headers;

    private final byte[] data;

    ResponseImpl(
        final HttpStatus status,
        final ContentType contentType,
        final Map<String, List<String>> headers,
        final byte[] data
    ) {
        this(
            status,
            contentType.getValue(),
            headers,
            data
        );
    }

    ResponseImpl(
        final HttpStatus status,
        final String contentType,
        final Map<String, List<String>> headers,
        final byte[] data
    ) {
        this.status = status;
        this.contentType = contentType;

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
        return ContentType.fromString(contentType);
    }

    @Override
    public String getContentTypeValue() {
        return contentType;
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
