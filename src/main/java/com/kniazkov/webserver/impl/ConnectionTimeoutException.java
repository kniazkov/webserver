/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

/**
 * Indicates that a client connection timed out while the server was waiting
 * for request data.
 * <p>
 * This exception represents a connection-level timeout rather than an HTTP
 * processing error. A worker receiving this exception should terminate the
 * connection without attempting to generate an HTTP error response.
 */
final class ConnectionTimeoutException extends ServerException {

    /**
     * Creates an exception indicating that the client connection timed out.
     *
     * @param cause
     *     the underlying exception that caused the timeout.
     */
    ConnectionTimeoutException(final Throwable cause) {
        super("Connection timed out", cause);
    }
}
