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
    public ResponseBuilder response() {
        return new ResponseBuilderImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder response(final HttpStatus status) {
        return new ResponseBuilderImpl()
            .setStatus(status);
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
        return createError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An internal server error occurred."
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response error(final ServerException exception) {
        final ServerException value = Objects.requireNonNull(
            exception,
            "Exception must not be null"
        );

        final HttpStatus status = value
            .getStatus()
            .orElse(HttpStatus.INTERNAL_SERVER_ERROR);

        final String message = value.getStatus().isPresent()
            ? value.getMessage()
            : "An internal server error occurred.";

        return createError(status, message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response error(final HttpStatus status) {
        final HttpStatus value = Objects.requireNonNull(
            status,
            "HTTP status must not be null"
        );
        return createError(value, value.getReason());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response error(
        final HttpStatus status,
        final String message
    ) {
        return createError(
            Objects.requireNonNull(
                status,
                "HTTP status must not be null"
            ),
            Objects.requireNonNull(
                message,
                "Error message must not be null"
            )
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromBytes(final byte[] data) {
        return response().setData(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromBytes(
        final byte[] data,
        final ContentType contentType
    ) {
        return response()
            .setContentType(contentType)
            .setData(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromBytes(
        final byte[] data,
        final String contentType
    ) throws ServerException {
        return response()
            .setContentType(contentType)
            .setData(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromText(final String value) {
        return response().setText(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromHtml(final String value) {
        return response().setHtml(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromJson(final String value) {
        return response().setJson(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder fromXml(final String value) {
        return response()
            .setContentType(ContentType.APPLICATION_XML)
            .setData(
                Objects.requireNonNull(
                    value,
                    "Response value must not be null"
                ).getBytes(StandardCharsets.UTF_8)
            );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder redirect(final String location)
        throws ServerException {
        return response(HttpStatus.FOUND)
            .setHeader("Location", location);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder redirectPermanently(
        final String location
    ) throws ServerException {
        return response(HttpStatus.MOVED_PERMANENTLY)
            .setHeader("Location", location);
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
            return error(
                new ServerException(
                    "Cannot read file: " + file,
                    exception
                )
            );
        }
    }

    /**
     * Creates an HTML error response.
     *
     * @param status
     *     the HTTP status.
     * @param message
     *     the error message.
     * @return
     *     the error response.
     */
    private Response createError(
        final HttpStatus status,
        final String message
    ) {
        if (!status.isError()) {
            throw new IllegalArgumentException(
                "Error response status must be an HTTP error"
            );
        }

        return new ResponseImpl(
            status,
            "text/html; charset=UTF-8",
            java.util.Map.of(),
            errorPage.create(
                status.getCode(),
                status.getReason(),
                message == null ? status.getReason() : message
            ).getBytes(StandardCharsets.UTF_8)
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
