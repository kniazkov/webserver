/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Creates an HTML page for an HTTP error response.
 */
public interface ErrorPage {

    /**
     * Creates an HTML page describing an HTTP error.
     *
     * @param code
     *     the HTTP status code.
     * @param reason
     *     the HTTP status reason phrase.
     * @param message
     *     the explanatory error message.
     * @return
     *     the complete HTML page.
     */
    String create(
        int code,
        String reason,
        String message
    );
}
