/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.ErrorPage;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Response;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a 404 Not Found response.
 */
final class NotFound implements Response {

    /**
     * The response data.
     */
    private final byte[] data;

    /**
     * Creates a 404 Not Found response.
     *
     * @param errorPage
     *     the error page generator.
     */
    NotFound(final ErrorPage errorPage) {
        Objects.requireNonNull(errorPage, "Error page must not be null");

        data = errorPage.create(
            HttpStatus.NOT_FOUND.getCode(),
            HttpStatus.NOT_FOUND.getReason(),
            "The requested resource was not found."
        ).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentType getContentType() {
        return ContentType.TEXT_HTML;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Charset> getCharset() {
        return Optional.of(StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getHeaders() {
        return Map.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getData() {
        return data.clone();
    }
}
