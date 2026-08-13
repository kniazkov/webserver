/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.ErrorPage;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ServerException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a 500 Internal Server Error response.
 */
final class InternalServerError implements Response {

    /**
     * The default error message.
     */
    private static final String DEFAULT_MESSAGE =
        "An internal server error occurred.";

    /**
     * The response data.
     */
    private final byte[] data;

    /**
     * Creates a 500 Internal Server Error response.
     *
     * @param errorPage
     *     the error page generator.
     */
    InternalServerError(final ErrorPage errorPage) {
        this(errorPage, DEFAULT_MESSAGE);
    }

    /**
     * Creates a 500 Internal Server Error response.
     *
     * @param errorPage
     *     the error page generator.
     * @param exception
     *     the server exception.
     */
    InternalServerError(
        final ErrorPage errorPage,
        final ServerException exception
    ) {
        this(
            errorPage,
            Objects.requireNonNull(
                exception,
                "Exception must not be null"
            ).getMessage()
        );
    }

    /**
     * Creates a 500 Internal Server Error response.
     *
     * @param errorPage
     *     the error page generator.
     * @param message
     *     the error message.
     */
    private InternalServerError(
        final ErrorPage errorPage,
        final String message
    ) {
        Objects.requireNonNull(
            errorPage,
            "Error page must not be null"
        );

        data = errorPage.create(
            HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReason(),
            message == null ? DEFAULT_MESSAGE : message
        ).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
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
