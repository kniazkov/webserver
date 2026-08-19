/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Handles HTTP requests.
 * <p>
 * Applications implement this interface to provide request processing logic.
 */
public interface Handler {

    /**
     * Handles an HTTP request.
     *
     * @param request
     *     the request.
     * @param environment
     *     the request processing environment.
     * @return
     *     the response.
     * @throws ServerException
     *     if the request cannot be processed.
     */
    Response handle(
        Request request,
        Environment environment
    ) throws ServerException;
}
