/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ResponseFactory;
import com.kniazkov.webserver.Server;
import com.kniazkov.webserver.ServerException;
import com.kniazkov.webserver.SslClientAuthentication;
import com.kniazkov.webserver.SslOptions;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * Default implementation of {@link Server}.
 * <p>
 * This class creates the listening socket, initializes the server environment
 * and runs the connection accept loop in a separate platform thread.
 * The platform thread keeps the JVM alive until {@link #stop()} is called.
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
            thread = Thread.ofPlatform()
                .name(
                    "webserver-" + serverSocket.getLocalPort()
                )
                .start(
                    () -> {
                        try {
                            ServerLoop.run(
                                serverSocket,
                                options,
                                environment
                            );
                        } catch (ServerException exception) {
                            /*
                             * The loop cannot report an asynchronous error to
                             * the caller of start(). Logging will belong here.
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
     * terminate. Interrupting the server thread also releases it when it is
     * waiting for an available worker. This method does not return until the
     * accept loop has terminated.
     *
     * @throws ServerException
     *     if the listening socket cannot be closed.
     */
    @Override
    public synchronized void stop() throws ServerException {
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException exception) {
                throw new ServerException(
                    "Cannot stop server",
                    exception
                );
            }
        }

        thread.interrupt();

        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServerException(
                "Interrupted while stopping server",
                exception
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetAddress getBindAddress() {
        return serverSocket.getInetAddress();
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
                options,
                options.getSslOptions().get()
            );
        }

        try {
            return ServerSocketFactory
                .getDefault()
                .createServerSocket(
                    options.getPort(),
                    options.getBacklog(),
                    options.getBindAddress().orElse(null)
                );
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
     * @param serverOptions
     *     the server options.
     * @param sslOptions
     *     the SSL/TLS options.
     * @return
     *     the SSL/TLS server socket.
     * @throws ServerException
     *     if SSL initialization fails.
     */
    private static ServerSocket createSslServerSocket(
        final Options serverOptions,
        final SslOptions sslOptions
    ) throws ServerException {
        try {
            final SSLContext context =
                SSLContext.getInstance(
                    sslOptions.getProtocol().getValue()
                );

            context.init(
                TlsMaterialLoader.loadKeyManagers(sslOptions),
                TlsMaterialLoader.loadTrustManagers(sslOptions),
                null
            );

            final SSLServerSocket socket = (SSLServerSocket) context
                .getServerSocketFactory()
                .createServerSocket(
                    serverOptions.getPort(),
                    serverOptions.getBacklog(),
                    serverOptions.getBindAddress().orElse(null)
                );

            try {
                configureSslServerSocket(socket, sslOptions);
            } catch (IllegalArgumentException exception) {
                close(socket);
                throw new ServerException(
                    "Invalid TLS listener policy: "
                        + exception.getMessage(),
                    exception
                );
            }
            return socket;
        } catch (
            IOException
            | GeneralSecurityException exception
        ) {
            throw new ServerException(
                "Cannot initialize SSL server: "
                    + exception.getMessage(),
                exception
            );
        }
    }

    /**
     * Applies the configured TLS versions, cipher suites and client policy.
     *
     * @param socket
     *     the SSL server socket.
     * @param options
     *     the SSL options.
     */
    private static void configureSslServerSocket(
        final SSLServerSocket socket,
        final SslOptions options
    ) {
        if (!options.getEnabledProtocols().isEmpty()) {
            socket.setEnabledProtocols(
                options.getEnabledProtocols()
                    .stream()
                    .map(value -> value.getValue())
                    .toArray(String[]::new)
            );
        }
        if (!options.getCipherSuites().isEmpty()) {
            socket.setEnabledCipherSuites(
                options.getCipherSuites().toArray(String[]::new)
            );
        }

        final SslClientAuthentication authentication =
            options.getClientAuthentication();
        if (authentication == SslClientAuthentication.REQUIRED) {
            socket.setNeedClientAuth(true);
        } else if (authentication == SslClientAuthentication.OPTIONAL) {
            socket.setWantClientAuth(true);
        }
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
            /*
             * Nothing useful can be done here.
             */
        }
    }
}
