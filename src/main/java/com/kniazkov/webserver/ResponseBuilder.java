/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Builds a custom HTTP response.
 */
public interface ResponseBuilder {

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
     * Builds an immutable HTTP response.
     *
     * @return
     *     the response.
     * @throws ServerException
     *     if a valid response cannot be built.
     */
    Response build() throws ServerException;
}
