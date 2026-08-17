/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Represents a running web server.
 * <p>
 * A server is created and started using {@link #start(Options)}. The returned
 * instance can later be used to stop the server.
 */
public interface Server {

    /**
     * Starts a web server using the specified options.
     *
     * @param options
     *     the server options.
     * @return
     *     the running server.
     * @throws ServerException
     *     if the server cannot be started.
     */
    static Server start(final Options options) throws ServerException {
        return com.kniazkov.webserver.impl.ServerImpl.start(options);
    }

    /**
     * Stops the server.
     * <p>
     * Calling this method on an already stopped server has no effect.
     *
     * @throws ServerException
     *     if the server cannot be stopped normally.
     */
    void stop() throws ServerException;
}
