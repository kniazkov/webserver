/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Represents an HTTP protocol version supported by the web server.
 */
public enum HttpVersion {

    /**
     * HTTP/1.0.
     */
    HTTP_1_0("HTTP/1.0"),

    /**
     * HTTP/1.1.
     */
    HTTP_1_1("HTTP/1.1");

    /**
     * The textual representation of the protocol version.
     */
    private final String text;

    /**
     * Creates a new HTTP version.
     *
     * @param text
     *     the textual representation of the protocol version.
     */
    HttpVersion(final String text) {
        this.text = text;
    }

    /**
     * Returns the textual representation of the protocol version.
     *
     * @return
     *     the textual representation.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the HTTP version corresponding to the specified text.
     *
     * @param value
     *     the textual representation of the HTTP version.
     * @return
     *     the corresponding HTTP version.
     * @throws ServerException
     *     if the specified version is not supported.
     */
    public static HttpVersion fromString(final String value) throws ServerException {
        final String normalized = value.trim();
        for (HttpVersion version : values()) {
            if (version.text.equals(normalized)) {
                return version;
            }
        }
        throw new ServerException(
            HttpStatus.HTTP_VERSION_NOT_SUPPORTED,
            "Unsupported HTTP version: " + value
        );
    }

    /**
     * Returns the textual representation of this protocol version.
     *
     * @return
     *     the textual representation.
     */
    @Override
    public String toString() {
        return text;
    }
}
