/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Represents an exception thrown by the web server.
 * <p>
 * This exception is used to distinguish server-specific errors from other
 * exceptions. The exception message describes the cause of the failure and can
 * be reported to the console or log.
 */
public class ServerException extends Exception {

    /**
     * Creates a new server exception with the specified message.
     *
     * @param message
     *     the detail message.
     */
    public ServerException(final String message) {
        super(message);
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
    }
}
