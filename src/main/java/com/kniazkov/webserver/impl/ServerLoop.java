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
import java.util.concurrent.TimeUnit;

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
     * Maximum time to wait before checking whether the listener was closed.
     */
    private static final long WORKER_WAIT_MILLIS = 100;

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
            if (!acquireWorker()) {
                return;
            }

            final Socket socket;

            try {
                socket = serverSocket.accept();
            } catch (IOException exception) {
                workers.release();

                if (serverSocket.isClosed()) {
                    return;
                }

                throw new ServerException(
                    "Cannot accept client connection",
                    exception
                );
            }

            if (serverSocket.isClosed()) {
                workers.release();
                close(socket);
                return;
            }

            start(socket);
        }
    }

    /**
     * Waits for capacity before accepting another connection.
     * <p>
     * A bounded wait keeps the loop responsive when the listening socket is
     * closed without interrupting the loop thread.
     *
     * @return
     *     {@code true} if a worker permit was acquired, or {@code false} if
     *     the server was stopped.
     * @throws ServerException
     *     if waiting is interrupted while the server is still running.
     */
    private boolean acquireWorker() throws ServerException {
        while (!serverSocket.isClosed()) {
            try {
                if (
                    workers.tryAcquire(
                        WORKER_WAIT_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                ) {
                    return true;
                }
            } catch (InterruptedException exception) {
                if (serverSocket.isClosed()) {
                    return false;
                }

                Thread.currentThread().interrupt();

                throw new ServerException(
                    "Server loop was interrupted",
                    exception
                );
            }
        }

        return false;
    }

    /**
     * Starts processing an accepted connection.
     *
     * @param socket
     *     the accepted client socket.
     */
    private void start(final Socket socket) {
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
