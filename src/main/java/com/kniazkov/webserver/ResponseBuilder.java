/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Builds a custom HTTP response.
 */
public interface ResponseBuilder {

    /**
     * Sets the HTTP status.
     *
     * @param status
     *     the HTTP status.
     * @return
     *     this builder.
     */
    ResponseBuilder setStatus(HttpStatus status);

    /**
     * Sets a known content type.
     *
     * @param contentType
     *     the content type.
     * @return
     *     this builder.
     */
    ResponseBuilder setContentType(ContentType contentType);

    /**
     * Sets an arbitrary Content-Type header value.
     *
     * @param contentType
     *     the media type, optionally including parameters.
     * @return
     *     this builder.
     * @throws ServerException
     *     if the value is invalid.
     */
    ResponseBuilder setContentType(String contentType)
        throws ServerException;

    /**
     * Replaces the response body with arbitrary bytes.
     * <p>
     * The supplied array is copied immediately.
     *
     * @param data
     *     the response body.
     * @return
     *     this builder.
     */
    ResponseBuilder setData(byte[] data);

    /**
     * Replaces the response body with UTF-8 plain text.
     *
     * @param value
     *     the response text.
     * @return
     *     this builder.
     */
    ResponseBuilder setText(String value);

    /**
     * Replaces the response body with UTF-8 HTML.
     *
     * @param value
     *     the HTML content.
     * @return
     *     this builder.
     */
    ResponseBuilder setHtml(String value);

    /**
     * Replaces the response body with UTF-8 JSON.
     *
     * @param value
     *     the JSON content.
     * @return
     *     this builder.
     */
    ResponseBuilder setJson(String value);

    /**
     * Adds a value to an HTTP response header.
     * <p>
     * If the header already exists, the value is added to its existing
     * values.
     *
     * @param name
     *     the header name.
     * @param value
     *     the header value.
     * @return
     *     this builder.
     * @throws ServerException
     *     if the header name or value is invalid.
     */
    ResponseBuilder addHeader(
        String name,
        String value
    ) throws ServerException;

    /**
     * Replaces all values of an HTTP response header.
     *
     * @param name
     *     the header name.
     * @param value
     *     the header value.
     * @return
     *     this builder.
     * @throws ServerException
     *     if the header name or value is invalid.
     */
    ResponseBuilder setHeader(
        String name,
        String value
    ) throws ServerException;

    /**
     * Sets a cookie.
     * <p>
     * If a cookie with the same name has already been set, it is replaced.
     *
     * @param name
     *     the cookie name.
     * @param value
     *     the cookie value.
     * @return
     *     this builder.
     * @throws ServerException
     *     if the cookie name or value is invalid.
     */
    ResponseBuilder setCookie(
        String name,
        String value
    ) throws ServerException;

    /**
     * Sets a cookie including its optional attributes.
     *
     * @param cookie
     *     the response cookie.
     * @return
     *     this builder.
     * @throws ServerException
     *     if the cookie is invalid.
     */
    ResponseBuilder setCookie(ResponseCookie cookie)
        throws ServerException;

    /**
     * Builds an immutable HTTP response.
     *
     * @return
     *     the response.
     * @throws ServerException
     *     if a valid response cannot be built.
     */
    Response build() throws ServerException;
}
