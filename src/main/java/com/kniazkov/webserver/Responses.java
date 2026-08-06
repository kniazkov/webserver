/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides factory methods for creating HTTP responses.
 */
public final class Responses {

    /**
     * The shared empty response body.
     */
    private static final byte[] EMPTY_DATA = new byte[0];

    /**
     * The shared response representing HTTP 204 No Content.
     */
    private static final Response NO_CONTENT = create(
        HttpStatus.NO_CONTENT,
        ContentType.APPLICATION_OCTET_STREAM,
        EMPTY_DATA
    );

    /**
     * The shared response representing HTTP 404 Not Found.
     */
    private static final Response NOT_FOUND = createText(
        HttpStatus.NOT_FOUND,
        "Not Found"
    );

    /**
     * The shared response representing HTTP 500 Internal Server Error.
     */
    private static final Response INTERNAL_SERVER_ERROR = createText(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal Server Error"
    );

    /**
     * Prevents instantiation of this utility class.
     */
    private Responses() {
    }

    /**
     * Creates an HTTP response.
     *
     * @param status
     *     the HTTP response status.
     * @param contentType
     *     the response content type.
     * @param data
     *     the response body data.
     * @return
     *     the created response.
     */
    public static Response create(
        final HttpStatus status,
        final ContentType contentType,
        final byte[] data
    ) {
        return create(status, contentType, Map.of(), data);
    }

    /**
     * Creates an HTTP response with additional header fields.
     *
     * @param status
     *     the HTTP response status.
     * @param contentType
     *     the response content type.
     * @param headers
     *     the additional response header fields.
     * @param data
     *     the response body data.
     * @return
     *     the created response.
     */
    public static Response create(
        final HttpStatus status,
        final ContentType contentType,
        final Map<String, List<String>> headers,
        final byte[] data
    ) {
        return new DefaultResponse(
            status,
            contentType,
            headers,
            data
        );
    }

    /**
     * Creates a successful HTTP response.
     *
     * @param contentType
     *     the response content type.
     * @param data
     *     the response body data.
     * @return
     *     the created response.
     */
    public static Response createOk(
        final ContentType contentType,
        final byte[] data
    ) {
        return create(HttpStatus.OK, contentType, data);
    }

    /**
     * Creates an HTTP response representing a newly created resource.
     *
     * @param contentType
     *     the response content type.
     * @param data
     *     the response body data.
     * @return
     *     the created response.
     */
    public static Response createCreated(
        final ContentType contentType,
        final byte[] data
    ) {
        return create(HttpStatus.CREATED, contentType, data);
    }

    /**
     * Creates a plain text HTTP response.
     *
     * @param status
     *     the HTTP response status.
     * @param text
     *     the response text.
     * @return
     *     the created response.
     */
    public static Response createText(
        final HttpStatus status,
        final String text
    ) {
        final String value = Objects.requireNonNull(
            text,
            "Response text must not be null."
        );

        return create(
            status,
            ContentType.TEXT_PLAIN,
            value.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Creates a redirect response.
     *
     * @param location
     *     the redirect target.
     * @return
     *     the created response.
     */
    public static Response createRedirect(final String location) {
        final String value = Objects.requireNonNull(
            location,
            "Redirect location must not be null."
        );

        return create(
            HttpStatus.FOUND,
            ContentType.APPLICATION_OCTET_STREAM,
            Map.of("Location", List.of(value)),
            EMPTY_DATA
        );
    }

    /**
     * Returns the shared HTTP 204 No Content response.
     *
     * @return
     *     the shared response.
     */
    public static Response getNoContent() {
        return NO_CONTENT;
    }

    /**
     * Returns the shared HTTP 404 Not Found response.
     *
     * @return
     *     the shared response.
     */
    public static Response getNotFound() {
        return NOT_FOUND;
    }

    /**
     * Returns the shared HTTP 500 Internal Server Error response.
     *
     * @return
     *     the shared response.
     */
    public static Response getInternalServerError() {
        return INTERNAL_SERVER_ERROR;
    }
}
