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
 * Represents a 403 Forbidden response.
 */
final class Forbidden implements Response {

    /**
     * The response data.
     */
    private final byte[] data;

    /**
     * Creates a 403 Forbidden response.
     *
     * @param errorPage
     *     the error page generator.
     */
    Forbidden(final ErrorPage errorPage) {
        Objects.requireNonNull(
            errorPage,
            "Error page must not be null"
        );

        data = errorPage.create(
            HttpStatus.FORBIDDEN.getCode(),
            HttpStatus.FORBIDDEN.getReason(),
            "Access to the requested resource is forbidden."
        ).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.TEXT_HTML;
    }

    @Override
    public Optional<Charset> getCharset() {
        return Optional.of(StandardCharsets.UTF_8);
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return Map.of();
    }

    @Override
    public byte[] getData() {
        return data.clone();
    }
}
