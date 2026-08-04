/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Represents an HTTP response status.
 */
public enum HttpStatus {

    /**
     * 200 OK.
     */
    OK(200, "OK"),

    /**
     * 201 Created.
     */
    CREATED(201, "Created"),

    /**
     * 204 No Content.
     */
    NO_CONTENT(204, "No Content"),

    /**
     * 301 Moved Permanently.
     */
    MOVED_PERMANENTLY(301, "Moved Permanently"),

    /**
     * 302 Found.
     */
    FOUND(302, "Found"),

    /**
     * 304 Not Modified.
     */
    NOT_MODIFIED(304, "Not Modified"),

    /**
     * 400 Bad Request.
     */
    BAD_REQUEST(400, "Bad Request"),

    /**
     * 401 Unauthorized.
     */
    UNAUTHORIZED(401, "Unauthorized"),

    /**
     * 403 Forbidden.
     */
    FORBIDDEN(403, "Forbidden"),

    /**
     * 404 Not Found.
     */
    NOT_FOUND(404, "Not Found"),

    /**
     * 405 Method Not Allowed.
     */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),

    /**
     * 413 Payload Too Large.
     */
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),

    /**
     * 415 Unsupported Media Type.
     */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

    /**
     * 500 Internal Server Error.
     */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),

    /**
     * 501 Not Implemented.
     */
    NOT_IMPLEMENTED(501, "Not Implemented"),

    /**
     * 503 Service Unavailable.
     */
    SERVICE_UNAVAILABLE(503, "Service Unavailable");

    /**
     * The numeric status code.
     */
    private final int code;

    /**
     * The standard reason phrase.
     */
    private final String reason;

    /**
     * Creates an HTTP status.
     *
     * @param code
     *     the numeric status code.
     * @param reason
     *     the standard reason phrase.
     */
    HttpStatus(final int code, final String reason) {
        this.code = code;
        this.reason = reason;
    }

    /**
     * Returns the numeric status code.
     *
     * @return
     *     the status code.
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the standard reason phrase.
     *
     * @return
     *     the reason phrase.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns the textual representation of this status.
     *
     * @return
     *     the textual representation.
     */
    @Override
    public String toString() {
        return code + " " + reason;
    }
}
