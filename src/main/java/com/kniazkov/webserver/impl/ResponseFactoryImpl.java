/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.ErrorPage;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseBuilder;
import com.kniazkov.webserver.ResponseFactory;
import com.kniazkov.webserver.ServerException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Default implementation of {@link ResponseFactory}.
 */
final class ResponseFactoryImpl implements ResponseFactory {

    /**
     * The error page generator.
     */
    private final ErrorPage errorPage;

    /**
     * The cached 404 Not Found response.
     */
    private final Response notFound;

    /**
     * Creates a response factory using the default error page.
     */
    public ResponseFactoryImpl() {
        this(DefaultErrorPage.getInstance());
    }

    /**
     * Creates a response factory.
     *
     * @param errorPage
     *     the error page generator.
     */
    public ResponseFactoryImpl(final ErrorPage errorPage) {
        this.errorPage = Objects.requireNonNull(
            errorPage,
            "Error page must not be null"
        );
        this.notFound = new NotFound(errorPage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response noResponse() {
        return NoResponse.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response notFound() {
        return notFound;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response error() {
        return new InternalServerError(errorPage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response error(final ServerException exception) {
        return new InternalServerError(
            errorPage,
            Objects.requireNonNull(
                exception,
                "Exception must not be null"
            )
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromText(final String value) {
        return builder(
            ContentType.TEXT_PLAIN,
            value
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromHtml(final String value) {
        return builder(
            ContentType.TEXT_HTML,
            value
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromJson(final String value) {
        return builder(
            ContentType.APPLICATION_JSON,
            value
        );
    }

    /**
     * Creates a successful response builder containing UTF-8 text.
     *
     * @param contentType
     *     the response content type.
     * @param value
     *     the response text.
     * @return
     *     the response builder.
     */
    private static ResponseBuilder builder(
        final ContentType contentType,
        final String value
    ) {
        Objects.requireNonNull(
            value,
            "Response value must not be null"
        );

        return new ResponseBuilderImpl(
            HttpStatus.OK,
            contentType,
            value.getBytes(StandardCharsets.UTF_8)
        );
    }
}
