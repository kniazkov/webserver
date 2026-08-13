/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Represents the environment available to a request handler.
 * <p>
 * Implementations of this interface are provided by the web server library.
 */
public interface Environment {

    /**
     * Returns the response factory.
     *
     * @return
     *     the response factory.
     */
    ResponseFactory getResponseFactory();
}
