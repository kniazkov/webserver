/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

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
    public static HttpMethod fromString(final String value)
        throws ServerException {
        for (HttpMethod method : values()) {
            if (method.text.equals(value)) {
                return method;
            }
        }

        if (!isToken(value)) {
            throw new ServerException(
                "Invalid HTTP method: " + value
            );
        }

        throw new ServerException(
            HttpStatus.NOT_IMPLEMENTED,
            "Unsupported HTTP method: " + value
        );
    }

    /**
     * Returns whether a value is a non-empty HTTP token.
     *
     * @param value
     *     the value.
     * @return
     *     {@code true} if the value is a valid token.
     */
    private static boolean isToken(final String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        for (int index = 0; index < value.length(); index++) {
            final char ch = value.charAt(index);

            if (
                !(ch >= '0' && ch <= '9')
                    && !(ch >= 'A' && ch <= 'Z')
                    && !(ch >= 'a' && ch <= 'z')
                    && "!#$%&'*+-.^_`|~".indexOf(ch) < 0
            ) {
                return false;
            }
        }

        return true;
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
