/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ServerException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.Semaphore;

/**
 * Accepts incoming network connections and dispatches them to HTTP workers.
 * <p>
 * The loop owns the supplied server socket and continuously accepts client
 * connections until the server socket is closed or an unrecoverable accept
 * error occurs.
 * <p>
 * Every accepted connection is processed by a separate virtual thread.
 * The number of concurrently active workers is limited by the configured
 * maximum worker count. A worker occupies one permit for the complete
 * lifetime of its client connection, including persistent HTTP connections.
 */
final class ServerLoop {

    /**
     * The listening server socket.
     */
    private final ServerSocket serverSocket;

    /**
     * The server options.
     */
    private final Options options;

    /**
     * The handler environment.
     */
    private final Environment environment;

    /**
     * Limits the number of concurrently active workers.
     */
    private final Semaphore workers;

    /**
     * Creates a server loop.
     *
     * @param serverSocket
     *     the listening server socket.
     * @param options
     *     the server options.
     * @param environment
     *     the handler environment.
     */
    private ServerLoop(
        final ServerSocket serverSocket,
        final Options options,
        final Environment environment
    ) {
        this.serverSocket = Objects.requireNonNull(
            serverSocket,
            "Server socket must not be null"
        );
        this.options = Objects.requireNonNull(
            options,
            "Options must not be null"
        );
        this.environment = Objects.requireNonNull(
            environment,
            "Environment must not be null"
        );
        workers = new Semaphore(options.getMaxWorkers());
    }

    /**
     * Starts processing incoming connections.
     * <p>
     * This method blocks until the server socket is closed or accepting a
     * connection fails.
     *
     * @param serverSocket
     *     the listening server socket.
     * @param options
     *     the server options.
     * @param environment
     *     the handler environment.
     * @throws ServerException
     *     if accepting a connection fails.
     */
    static void run(
        final ServerSocket serverSocket,
        final Options options,
        final Environment environment
    ) throws ServerException {
        new ServerLoop(
            serverSocket,
            options,
            environment
        ).run();
    }

    /**
     * Runs the accept loop.
     *
     * @throws ServerException
     *     if accepting a connection fails.
     */
    private void run() throws ServerException {
        while (!serverSocket.isClosed()) {
            final Socket socket;

            try {
                socket = serverSocket.accept();
            } catch (IOException exception) {
                if (serverSocket.isClosed()) {
                    return;
                }

                throw new ServerException(
                    "Cannot accept client connection",
                    exception
                );
            }

            start(socket);
        }
    }

    /**
     * Starts processing an accepted connection.
     *
     * @param socket
     *     the accepted client socket.
     * @throws ServerException
     *     if waiting for an available worker is interrupted.
     */
    private void start(final Socket socket)
        throws ServerException {
        try {
            workers.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            close(socket);

            throw new ServerException(
                "Server loop was interrupted",
                exception
            );
        }

        try {
            Thread.startVirtualThread(
                () -> {
                    try {
                        new Worker(
                            socket,
                            options,
                            environment
                        ).run();
                    } finally {
                        workers.release();
                    }
                }
            );
        } catch (RuntimeException exception) {
            workers.release();
            close(socket);
            throw exception;
        }
    }

    /**
     * Silently closes a client socket.
     *
     * @param socket
     *     the socket.
     */
    private static void close(final Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Nothing useful can be done here.
        }
    }
}
