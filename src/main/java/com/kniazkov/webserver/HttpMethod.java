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
     * Retrieves a resource from the server.
     */
    GET("GET"),

    /**
     * Sends data to the server.
     */
    POST("POST");

    /**
     * The textual representation of the method.
     */
    private final String text;

    /**
     * Creates a new HTTP method.
     *
     * @param text
     *     the textual representation of the method.
     */
    HttpMethod(final String text) {
        this.text = text;
    }

    /**
     * Returns the textual representation of the method.
     *
     * @return
     *     the textual representation.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the HTTP method corresponding to the specified text.
     *
     * @param value
     *     the textual representation of the HTTP method.
     * @return
     *     the corresponding HTTP method.
     * @throws ServerException
     *     if the specified method is not supported.
     */
    public static HttpMethod fromString(final String value) throws ServerException {
        final String normalized = value.trim().toUpperCase(Locale.ENGLISH);
        for (HttpMethod method : values()) {
            if (method.text.equals(normalized)) {
                return method;
            }
        }
        throw new ServerException(
            HttpStatus.NOT_IMPLEMENTED,
            "Unsupported HTTP method: " + value
        );
    }

    /**
     * Returns the textual representation of this method.
     *
     * @return
     *     the textual representation.
     */
    @Override
    public String toString() {
        return text;
    }
}
