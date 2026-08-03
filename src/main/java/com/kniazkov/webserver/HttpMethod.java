/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.Locale;

/**
 * Represents an HTTP request method supported by the web server.
 */
public enum HttpMethod {

    /**
     * Retrieves resource from the server.
     */
    GET,

    /**
     * Sends data to the server.
     */
    POST;

    /**
     * Returns the HTTP method corresponding to the specified text.
     *
     * @param value the textual representation of the HTTP method
     * @return the corresponding HTTP method
     * @throws ServerException if the specified method is not supported
     */
    public static HttpMethod fromString(final String value) throws ServerException {
        return switch (value.trim().toUpperCase(Locale.ENGLISH)) {
            case "GET" -> GET;
            case "POST" -> POST;
            default -> throw new ServerException(
                "Unsupported HTTP method: " + value
            );
        };
    }
}
