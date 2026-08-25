/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseFactory;
import com.kniazkov.webserver.ServerException;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Processes HTTP requests received through one client connection.
 * <p>
 * A worker owns one accepted socket and processes requests sequentially until
 * the client closes the connection, HTTP persistence is disabled, a timeout
 * occurs, or an unrecoverable communication error is detected.
 * <p>
 * The same {@link SocketByteSource} is used for the entire lifetime of the
 * connection. This is important because the source may buffer bytes belonging
 * to subsequent HTTP requests.
 */
final class Worker implements Runnable {

    /**
     * The complete HTTP/1.1 continue interim response.
     */
    private static final byte[] CONTINUE_RESPONSE =
        "HTTP/1.1 100 Continue\r\n\r\n".getBytes(
            StandardCharsets.ISO_8859_1
        );

    /**
     * The client socket.
     */
    private final Socket socket;

    /**
     * The server options.
     */
    private final Options options;

    /**
     * The handler environment.
     */
    private final Environment environment;

    /**
     * The response factory.
     */
    private final ResponseFactory responseFactory;

    /**
     * Creates a worker.
     *
     * @param socket
     *     the accepted client socket.
     * @param options
     *     the server options.
     * @param environment
     *     the handler environment.
     */
    Worker(
        final Socket socket,
        final Options options,
        final Environment environment
    ) {
        this.socket = socket;
        this.options = options;
        this.environment = environment;
        responseFactory = environment.getResponseFactory();
    }

    /**
     * Processes the client connection.
     */
    @Override
    public void run() {
        try (socket) {
            configureSocket();

            final ByteSource source = new SocketByteSource(socket);
            final SocketResponseWriter writer =
                new SocketResponseWriter(
                    socket,
                    options.getWriteTimeout()
                );

            while (true) {
                final Request request;

                try {
                    request = RequestParser.parse(
                        source,
                        options,
                        () -> writer.write(CONTINUE_RESPONSE)
                    );
                } catch (ConnectionClosedException exception) {
                    return;
                } catch (ConnectionTimeoutException exception) {
                    return;
                } catch (ServerException exception) {
                    writeError(writer, exception, HttpVersion.HTTP_1_1);
                    return;
                }

                final boolean keepAlive = isKeepAlive(request);

                try {
                    final Response response = process(request);

                    writeResponse(
                        writer,
                        response,
                        request.getHeaders().getVersion(),
                        keepAlive
                    );
                } finally {
                    closeRequest(request);
                }

                if (!keepAlive) {
                    return;
                }
            }
        } catch (SocketTimeoutException exception) {
            // The client stopped sending data. Close the connection.
        } catch (IOException | ServerException exception) {
            // Connection-level failure. Logging will belong here.
        }
    }

    /**
     * Configures the accepted socket.
     *
     * @throws IOException
     *     if socket configuration fails.
     */
    private void configureSocket() throws IOException {
        final long timeout = options.getReadTimeout().toMillis();

        if (timeout > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "Socket read timeout is too large"
            );
        }

        socket.setSoTimeout((int) timeout);
        socket.setTcpNoDelay(true);
    }

    /**
     * Processes one parsed HTTP request.
     *
     * @param request
     *     the request.
     * @return
     *     the response.
     */
    private Response process(final Request request) {
        try {
            Response response = invokeHandler(request);

            if (response == NoResponse.getInstance()) {
                response = defaultResponse(request);
            }

            return response;
        } catch (ServerException exception) {
            return responseFactory.error(exception);
        }
    }

    /**
     * Invokes the user handler with the configured timeout.
     *
     * @param request
     *     the request.
     * @return
     *     the handler response.
     * @throws ServerException
     *     if handler execution fails or times out.
     */
    private Response invokeHandler(final Request request)
        throws ServerException {
        final Handler handler = options.getHandler();

        final FutureTask<Response> task = new FutureTask<>(
            () -> handler.handle(request, environment)
        );

        final Thread thread = Thread.startVirtualThread(task);

        try {
            return task.get(
                options.getHandlerTimeout().toMillis(),
                TimeUnit.MILLISECONDS
            );
        } catch (TimeoutException exception) {
            task.cancel(true);
            thread.interrupt();

            throw new ServerException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Request handler execution timed out",
                exception
            );
        } catch (InterruptedException exception) {
            task.cancel(true);
            Thread.currentThread().interrupt();

            throw new ServerException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Request handler execution was interrupted",
                exception
            );
        } catch (ExecutionException exception) {
            final Throwable cause = exception.getCause();

            if (cause instanceof ServerException serverException) {
                throw serverException;
            }

            throw new ServerException(
                "Request handler execution failed",
                cause
            );
        }
    }

    /**
     * Applies the default response algorithm.
     *
     * @param request
     *     the request.
     * @return
     *     the response.
     */
    private Response defaultResponse(final Request request) {
        final Path root = Path.of(options.getWwwRoot())
            .toAbsolutePath()
            .normalize();

        final String path = request.getPath().getPath();

        final Path resolved = root.resolve(
            path.substring(1)
        ).normalize();

        if (!resolved.startsWith(root)) {
            return responseFactory.notFound();
        }

        try {
            final Path realRoot = root.toRealPath();
            final Path realFile = resolved.toRealPath();

            if (!realFile.startsWith(realRoot)) {
                return responseFactory.notFound();
            }

            return responseFactory.fromFile(realFile.toFile());
        } catch (NoSuchFileException exception) {
            return responseFactory.notFound();
        } catch (IOException | SecurityException exception) {
            return responseFactory.forbidden();
        }
    }

    /**
     * Writes an HTTP response.
     *
     * @param writer
     *     the bounded socket response writer.
     * @param response
     *     the response.
     * @param version
     *     the HTTP version.
     * @param keepAlive
     *     whether the connection remains open after the response.
     * @throws IOException
     *     if writing fails.
     * @throws ServerException
     *     if the response contains invalid application headers.
     */
    private static void writeResponse(
        final SocketResponseWriter writer,
        final Response response,
        final HttpVersion version,
        final boolean keepAlive
    ) throws IOException, ServerException {
        writer.write(
            ResponseSerializer.serialize(
                response,
                version,
                keepAlive
            )
        );
    }

    /**
     * Writes an internal server error response.
     *
     * @param writer
     *     the bounded socket response writer.
     * @param exception
     *     the error.
     * @param version
     *     the HTTP version.
     * @throws IOException
     *     if writing fails.
     * @throws ServerException
     *     if the generated response is invalid.
     */
    private void writeError(
        final SocketResponseWriter writer,
        final ServerException exception,
        final HttpVersion version
    ) throws IOException, ServerException {
        writeResponse(
            writer,
            responseFactory.error(exception),
            version,
            false
        );
    }

    /**
     * Releases storage owned by a parsed request.
     *
     * @param request
     *     the parsed request.
     * @throws ServerException
     *     if request storage cannot be released.
     */
    private static void closeRequest(final Request request)
        throws ServerException {
        if (request instanceof ManagedRequest managed) {
            managed.close();
        }
    }

    /**
     * Returns whether the connection should remain open.
     *
     * @param request
     *     the request.
     * @return
     *     {@code true} if another request may be received.
     */
    private static boolean isKeepAlive(final Request request) {
        final List<String> values =
            request.getHeaders().getValues().get("Connection");

        if (containsToken(values, "close")) {
            return false;
        }

        return request.getHeaders().getVersion() == HttpVersion.HTTP_1_1
            || containsToken(values, "keep-alive");
    }

    /**
     * Checks whether header values contain a comma-separated token.
     *
     * @param values
     *     the header values.
     * @param expected
     *     the expected token.
     * @return
     *     whether the token is present.
     */
    private static boolean containsToken(
        final List<String> values,
        final String expected
    ) {
        if (values == null) {
            return false;
        }

        for (String value : values) {
            for (String token : value.split(",")) {
                if (
                    token.trim().toLowerCase(Locale.ENGLISH)
                        .equals(expected)
                ) {
                    return true;
                }
            }
        }

        return false;
    }
}
