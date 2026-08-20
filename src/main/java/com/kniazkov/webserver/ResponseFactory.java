/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.io.File;

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
     * Returns a {@code 403 Forbidden} response.
     *
     * @return
     *     the response.
     */
    Response forbidden();

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
     * Returns an error response with the specified status.
     *
     * @param status
     *     the HTTP error status.
     * @return
     *     the response.
     */
    Response error(HttpStatus status);

    /**
     * Returns an error response with the specified status and message.
     *
     * @param status
     *     the HTTP error status.
     * @param message
     *     the client-visible error message.
     * @return
     *     the response.
     */
    Response error(HttpStatus status, String message);

    /**
     * Creates a {@code 200 OK} response builder containing arbitrary bytes
     * with the {@code application/octet-stream} content type.
     *
     * @param data
     *     the response body.
     * @return
     *     the response builder.
     */
    ResponseBuilder fromBytes(byte[] data);

    /**
     * Creates a fully custom response builder.
     * <p>
     * This is the only factory method that allows the status, content type,
     * and body to be selected independently. Prefer a more specific factory
     * method whenever one matches the response being created.
     *
     * @param status
     *     the HTTP status.
     * @param contentType
     *     the complete Content-Type value.
     * @param data
     *     the response body.
     * @return
     *     the response builder.
     * @throws ServerException
     *     if the content type is invalid.
     */
    ResponseBuilder custom(
        HttpStatus status,
        String contentType,
        byte[] data
    ) throws ServerException;

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

    /**
     * Creates an XML response builder.
     *
     * @param value
     *     the XML content.
     * @return
     *     the response builder.
     */
    ResponseBuilder fromXml(String value);

    /**
     * Creates a temporary redirect response builder.
     *
     * @param location
     *     the redirect target.
     * @return
     *     the response builder.
     * @throws ServerException
     *     if the location cannot be used as a header value.
     */
    ResponseBuilder redirect(String location)
        throws ServerException;

    /**
     * Creates a permanent redirect response builder.
     *
     * @param location
     *     the redirect target.
     * @return
     *     the response builder.
     * @throws ServerException
     *     if the location cannot be used as a header value.
     */
    ResponseBuilder redirectPermanently(String location)
        throws ServerException;

    /**
     * Creates a response containing the specified file.
     *
     * @param file
     *     the file.
     * @return
     *     the resulting response.
     */
    Response fromFile(File file);
}
