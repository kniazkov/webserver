/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Creates HTTP responses and response builders.
 * <p>
 * Implementations of this interface are provided by the web server library.
 */
public interface ResponseFactory {

    /**
     * Returns an empty successful response.
     *
     * @return
     *     the response.
     */
    Response noResponse();

    /**
     * Returns a {@code 404 Not Found} response.
     *
     * @return
     *     the response.
     */
    Response notFound();

    /**
     * Returns a {@code 500 Internal Server Error} response.
     *
     * @return
     *     the response.
     */
    Response error();

    /**
     * Returns a {@code 500 Internal Server Error} response for the specified
     * server exception.
     *
     * @param exception
     *     the server exception.
     * @return
     *     the response.
     */
    Response error(ServerException exception);

    /**
     * Creates a plain text response builder.
     *
     * @param value
     *     the response text.
     * @return
     *     the response builder.
     */
    ResponseBuilder fromText(String value);

    /**
     * Creates an HTML response builder.
     *
     * @param value
     *     the HTML content.
     * @return
     *     the response builder.
     */
    ResponseBuilder fromHtml(String value);

    /**
     * Creates a JSON response builder.
     *
     * @param value
     *     the JSON content.
     * @return
     *     the response builder.
     */
    ResponseBuilder fromJson(String value);
}
