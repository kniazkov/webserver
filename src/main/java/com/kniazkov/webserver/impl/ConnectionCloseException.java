/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

/**
 * Indicates that a connection was closed before a new HTTP request started.
 * <p>
 * This exception represents a normal termination of a persistent connection,
 * rather than an invalid or incomplete HTTP request.
 */
final class ConnectionClosedException extends ServerException {

    /**
     * Creates an exception indicating that the connection was closed.
     */
    ConnectionClosedException() {
        super("Connection closed");
    }
}
