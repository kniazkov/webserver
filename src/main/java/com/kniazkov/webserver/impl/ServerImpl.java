/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ResponseFactory;
import com.kniazkov.webserver.Server;
import com.kniazkov.webserver.ServerException;
import com.kniazkov.webserver.SslOptions;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Objects;

/**
 * Default implementation of {@link Server}.
 * <p>
 * This class creates the listening socket, initializes the server environment
 * and runs the connection accept loop in a separate virtual thread.
 * <p>
 * Depending on the supplied {@link Options}, either a regular TCP server
 * socket or an SSL/TLS server socket is created. The rest of the server does
 * not depend on the transport type.
 */
public final class ServerImpl implements Server {

    /**
     * The listening server socket.
     */
    private final ServerSocket serverSocket;

    /**
     * The server loop thread.
     */
    private final Thread thread;

    /**
     * Creates and starts a server.
     *
     * @param options
     *     the server options.
     * @return
     *     the running server.
     * @throws ServerException
     *     if the server cannot be created or started.
     */
    public static Server start(final Options options)
        throws ServerException {

        Objects.requireNonNull(
            options,
            "Options must not be null"
        );

        final ServerSocket serverSocket =
            createServerSocket(options);

        final Environment environment =
            new EnvironmentImpl(options);

        final Thread thread;

        try {
            thread = Thread.startVirtualThread(
                () -> {
                    try {
                        ServerLoop.run(
                            serverSocket,
                            options,
                            environment
                        );
                    } catch (ServerException exception) {
                        /*
                         * The loop cannot report an asynchronous error to the
                         * caller of start(). Logging will belong here.
                         */
                    }
                }
            );
        } catch (RuntimeException exception) {
            close(serverSocket);

            throw new ServerException(
                "Cannot start server thread",
                exception
            );
        }

        return new ServerImpl(
            serverSocket,
            thread
        );
    }

    /**
     * Creates a server instance.
     *
     * @param serverSocket
     *     the listening server socket.
     * @param thread
     *     the server loop thread.
     */
    private ServerImpl(
        final ServerSocket serverSocket,
        final Thread thread
    ) {
        this.serverSocket = serverSocket;
        this.thread = thread;
    }

    /**
     * Stops the server.
     * <p>
     * Closing the listening socket interrupts a blocking
     * {@link ServerSocket#accept()} call and causes the server loop to
     * terminate. Existing workers own their accepted sockets and therefore
     * finish independently.
     *
     * @throws ServerException
     *     if the listening socket cannot be closed.
     */
    @Override
    public void stop() throws ServerException {
        if (serverSocket.isClosed()) {
            return;
        }

        try {
            serverSocket.close();
        } catch (IOException exception) {
            throw new ServerException(
                "Cannot stop server",
                exception
            );
        }
    }

    /**
     * Creates the listening server socket.
     *
     * @param options
     *     the server options.
     * @return
     *     the server socket.
     * @throws ServerException
     *     if the socket cannot be created.
     */
    private static ServerSocket createServerSocket(
        final Options options
    ) throws ServerException {

        if (options.getSslOptions().isPresent()) {
            return createSslServerSocket(
                options.getPort(),
                options.getSslOptions().get()
            );
        }

        try {
            return ServerSocketFactory
                .getDefault()
                .createServerSocket(options.getPort());
        } catch (IOException exception) {
            throw new ServerException(
                "Cannot create server socket on port "
                    + options.getPort(),
                exception
            );
        }
    }

    /**
     * Creates an SSL/TLS server socket.
     *
     * @param port
     *     the server port.
     * @param options
     *     the SSL/TLS options.
     * @return
     *     the SSL/TLS server socket.
     * @throws ServerException
     *     if SSL initialization fails.
     */
    private static ServerSocket createSslServerSocket(
        final int port,
        final SslOptions options
    ) throws ServerException {

        final char[] storePassword =
            options.getKeyStorePassword();

        final char[] keyPassword =
            options.getKeyPassword();

        try {
            final KeyStore keyStore = KeyStore.getInstance(
                options.getKeyStoreType().getValue()
            );

            try (
                FileInputStream input =
                    new FileInputStream(options.getKeyStoreFile())
            ) {
                keyStore.load(
                    input,
                    storePassword
                );
            }

            final KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
                );

            keyManagerFactory.init(
                keyStore,
                keyPassword
            );

            final SSLContext context =
                SSLContext.getInstance(
                    options.getProtocol().getValue()
                );

            context.init(
                keyManagerFactory.getKeyManagers(),
                null,
                null
            );

            return context
                .getServerSocketFactory()
                .createServerSocket(port);

        } catch (
            IOException
            | GeneralSecurityException exception
        ) {
            throw new ServerException(
                "Cannot initialize SSL server",
                exception
            );
        } finally {
            clear(storePassword);
            clear(keyPassword);
        }
    }

    /**
     * Clears a password copy after it is no longer needed.
     *
     * @param value
     *     the password.
     */
    private static void clear(final char[] value) {
        java.util.Arrays.fill(value, '\0');
    }

    /**
     * Silently closes a server socket.
     *
     * @param socket
     *     the server socket.
     */
    private static void close(final ServerSocket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Nothing useful can be done here.
        }
    }
}
