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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
     * The cached 403 Forbidden response.
     */
    private final Response forbidden;

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
        this.forbidden = new Forbidden(errorPage);
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
    public Response forbidden() {
        return forbidden;
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

    @Override
    public Response fromFile(final File file) {
        Objects.requireNonNull(file, "File must not be null");

        if (!file.exists()) {
            return notFound;
        }

        if (!file.isFile() || !file.canRead()) {
            return forbidden;
        }

        try {
            final String name = file.getName();
            final int dot = name.lastIndexOf('.');

            final String extension = dot < 0
                ? ""
                : name.substring(dot + 1);

            final ContentType contentType =
                ContentType.fromExtension(extension);

            final ResponseBuilder builder = new ResponseBuilderImpl(
                HttpStatus.OK,
                contentType,
                Files.readAllBytes(file.toPath())
            );

            builder.setHeader(
                "Content-Disposition",
                "inline; filename=\"" + escapeFileName(name) + "\""
            );

            return builder.build();
        } catch (IOException | ServerException exception) {
            return new InternalServerError(
                errorPage,
                new ServerException(
                    "Cannot read file: " + file,
                    exception
                )
            );
        }
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

    /**
     * Escapes a file name for use in a quoted HTTP header parameter.
     *
     * @param value
     *     the file name.
     * @return
     *     the escaped file name.
     */
    private static String escapeFileName(final String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
}
