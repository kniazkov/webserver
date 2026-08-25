/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.net.InetAddress;

/**
 * Represents a running web server.
 * <p>
 * A server is created and started using {@link #start(Options)}. The returned
 * instance can later be used to stop the server. A running server keeps the
 * JVM alive until {@link #stop()} is called.
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
     * This method waits for the server accept loop to terminate. Calling it on
     * an already stopped server has no effect.
     *
     * @throws ServerException
     *     if the server cannot be stopped normally.
     */
    void stop() throws ServerException;

    /**
     * Returns the actual TCP port on which the server is listening.
     *
     * @return
     *     the actual port number.
     */
    int getPort();

    /**
     * Returns the local address on which the server is listening.
     *
     * @return
     *     the actual local bind address.
     */
    InetAddress getBindAddress();
}
