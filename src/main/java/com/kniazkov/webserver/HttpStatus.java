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
     * 202 Accepted.
     */
    ACCEPTED(202, "Accepted"),

    /**
     * 204 No Content.
     */
    NO_CONTENT(204, "No Content"),

    /**
     * 206 Partial Content.
     */
    PARTIAL_CONTENT(206, "Partial Content"),

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
     * 307 Temporary Redirect.
     */
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),

    /**
     * 308 Permanent Redirect.
     */
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

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
     * 406 Not Acceptable.
     */
    NOT_ACCEPTABLE(406, "Not Acceptable"),

    /**
     * 408 Request Timeout.
     */
    REQUEST_TIMEOUT(408, "Request Timeout"),

    /**
     * 409 Conflict.
     */
    CONFLICT(409, "Conflict"),

    /**
     * 410 Gone.
     */
    GONE(410, "Gone"),

    /**
     * 411 Length Required.
     */
    LENGTH_REQUIRED(411, "Length Required"),

    /**
     * 413 Payload Too Large.
     */
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),

    /**
     * 415 Unsupported Media Type.
     */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

    /**
     * 416 Range Not Satisfiable.
     */
    RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),

    /**
     * 422 Unprocessable Content.
     */
    UNPROCESSABLE_CONTENT(422, "Unprocessable Content"),

    /**
     * 426 Upgrade Required.
     */
    UPGRADE_REQUIRED(426, "Upgrade Required"),

    /**
     * 429 Too Many Requests.
     */
    TOO_MANY_REQUESTS(429, "Too Many Requests"),

    /**
     * 431 Request Header Fields Too Large.
     */
    REQUEST_HEADER_FIELDS_TOO_LARGE(
        431,
        "Request Header Fields Too Large"
    ),

    /**
     * 500 Internal Server Error.
     */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),

    /**
     * 501 Not Implemented.
     */
    NOT_IMPLEMENTED(501, "Not Implemented"),

    /**
     * 502 Bad Gateway.
     */
    BAD_GATEWAY(502, "Bad Gateway"),

    /**
     * 503 Service Unavailable.
     */
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),

    /**
     * 504 Gateway Timeout.
     */
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),

    /**
     * 505 HTTP Version Not Supported.
     */
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported");

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
     * Returns the HTTP status corresponding to a numeric status code.
     *
     * @param code
     *     the numeric status code.
     * @return
     *     the corresponding HTTP status.
     * @throws IllegalArgumentException
     *     if the status code is not represented by this enum.
     */
    public static HttpStatus fromCode(final int code) {
        for (HttpStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        throw new IllegalArgumentException(
            "Unsupported HTTP status code: " + code
        );
    }

    /**
     * Returns whether this status permits a response body.
     *
     * @return
     *     {@code true} if a response body is permitted.
     */
    public boolean allowsBody() {
        return code != 204 && code != 304;
    }

    /**
     * Returns whether this is a client or server error status.
     *
     * @return
     *     {@code true} for status codes in the range 400 through 599.
     */
    public boolean isError() {
        return code >= 400 && code <= 599;
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
