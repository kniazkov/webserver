/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents an exception thrown by the web server.
 * <p>
 * This exception is used to distinguish server-specific errors from other
 * exceptions. The exception message describes the cause of the failure and can
 * be reported to the console or log.
 */
public class ServerException extends Exception {

    /**
     * The serialization version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The HTTP status to return to the client, if this exception represents a
     * client-visible HTTP error.
     */
    private final HttpStatus status;

    /**
     * Creates a new server exception with the specified message.
     *
     * @param message
     *     the detail message.
     */
    public ServerException(final String message) {
        super(message);
        status = null;
    }

    /**
     * Creates a new server exception with the specified message and cause.
     *
     * @param message
     *     the detail message.
     * @param cause
     *     the cause of this exception.
     */
    public ServerException(final String message, final Throwable cause) {
        super(message, cause);
        status = null;
    }

    /**
     * Creates a server exception representing an HTTP error.
     *
     * @param status
     *     the HTTP status to return to the client.
     * @param message
     *     the client-visible detail message.
     */
    public ServerException(
        final HttpStatus status,
        final String message
    ) {
        super(message);
        this.status = validate(status);
    }

    /**
     * Creates a server exception representing an HTTP error.
     *
     * @param status
     *     the HTTP status to return to the client.
     * @param message
     *     the client-visible detail message.
     * @param cause
     *     the cause of this exception.
     */
    public ServerException(
        final HttpStatus status,
        final String message,
        final Throwable cause
    ) {
        super(message, cause);
        this.status = validate(status);
    }

    /**
     * Returns the HTTP status associated with this exception.
     *
     * @return
     *     the HTTP status, or an empty optional for an internal error.
     */
    public Optional<HttpStatus> getStatus() {
        return Optional.ofNullable(status);
    }

    /**
     * Validates a client-visible exception status.
     *
     * @param value
     *     the status to validate.
     * @return
     *     the validated status.
     */
    private static HttpStatus validate(final HttpStatus value) {
        final HttpStatus result = Objects.requireNonNull(
            value,
            "HTTP status must not be null"
        );

        if (!result.isError()) {
            throw new IllegalArgumentException(
                "Server exception status must be an HTTP error"
            );
        }

        return result;
    }
}
