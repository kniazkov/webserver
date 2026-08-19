/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ResponseFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Base class for tests of {@link Worker}.
 * <p>
 * Provides helpers for creating a real local TCP connection, starting a worker
 * for the accepted side of that connection, sending HTTP requests and reading
 * individual HTTP responses.
 * <p>
 * Responses are read according to their {@code Content-Length}, so the helpers
 * can also be used for persistent connections where several requests and
 * responses are exchanged through the same socket.
 */
abstract class WorkerBaseTest {

    /**
     * Default timeout used by tests.
     */
    protected static final Duration TIMEOUT =
        Duration.ofSeconds(2);

    /**
     * Creates a connection and starts a worker.
     *
     * @param options
     *     the server options.
     * @return
     *     the test connection.
     * @throws Exception
     *     if the connection cannot be created.
     */
    protected static Connection connect(final Options options)
        throws Exception {

        try (ServerSocket server = new ServerSocket(0)) {
            final Socket client = new Socket(
                "127.0.0.1",
                server.getLocalPort()
            );

            client.setSoTimeout(
                Math.toIntExact(TIMEOUT.toMillis())
            );

            final Socket accepted = server.accept();

            final Environment environment =
                new EnvironmentImpl(options);

            final Thread worker = Thread.startVirtualThread(
                new Worker(
                    accepted,
                    options,
                    environment
                )
            );

            return new Connection(client, worker);
        }
    }

    /**
     * Sends an HTTP request.
     *
     * @param socket
     *     the client socket.
     * @param request
     *     the request.
     * @throws IOException
     *     if writing fails.
     */
    protected static void send(
        final Socket socket,
        final String request
    ) throws IOException {
        socket.getOutputStream().write(
            request.getBytes(StandardCharsets.US_ASCII)
        );
        socket.getOutputStream().flush();
    }

    /**
     * Sends arbitrary request bytes.
     *
     * @param socket
     *     the client socket.
     * @param request
     *     the request bytes.
     * @throws IOException
     *     if writing fails.
     */
    protected static void send(
        final Socket socket,
        final byte[] request
    ) throws IOException {
        socket.getOutputStream().write(request);
        socket.getOutputStream().flush();
    }

    /**
     * Reads one complete HTTP response.
     *
     * @param socket
     *     the client socket.
     * @return
     *     the parsed response.
     * @throws IOException
     *     if reading fails or the response is incomplete.
     */
    protected static TestResponse readResponse(final Socket socket)
        throws IOException {

        final InputStream input = socket.getInputStream();

        final String statusLine = readLine(input);

        if (statusLine == null) {
            throw new IOException(
                "Connection closed before HTTP response"
            );
        }

        final Map<String, String> headers =
            new LinkedHashMap<>();

        while (true) {
            final String line = readLine(input);

            if (line == null) {
                throw new IOException(
                    "Connection closed inside HTTP headers"
                );
            }

            if (line.isEmpty()) {
                break;
            }

            final int colon = line.indexOf(':');

            if (colon <= 0) {
                throw new IOException(
                    "Invalid HTTP response header: " + line
                );
            }

            headers.put(
                line.substring(0, colon)
                    .trim()
                    .toLowerCase(Locale.ENGLISH),
                line.substring(colon + 1).trim()
            );
        }

        final String lengthValue =
            headers.get("content-length");

        if (lengthValue == null) {
            throw new IOException(
                "Content-Length header is missing"
            );
        }

        final int length;

        try {
            length = Integer.parseInt(lengthValue);
        } catch (NumberFormatException exception) {
            throw new IOException(
                "Invalid Content-Length: " + lengthValue,
                exception
            );
        }

        final byte[] body = input.readNBytes(length);

        if (body.length != length) {
            throw new IOException(
                "Connection closed inside HTTP response body"
            );
        }

        return new TestResponse(
            statusLine,
            Map.copyOf(headers),
            body
        );
    }

    /**
     * Reads one CRLF-terminated line.
     *
     * @param input
     *     the input stream.
     * @return
     *     the line without CRLF, or {@code null} if the stream ended before
     *     any data was read.
     * @throws IOException
     *     if reading fails or the line is incomplete.
     */
    private static String readLine(final InputStream input)
        throws IOException {

        final ByteArrayOutputStream buffer =
            new ByteArrayOutputStream();

        boolean cr = false;

        while (true) {
            final int value = input.read();

            if (value == -1) {
                if (buffer.size() == 0 && !cr) {
                    return null;
                }

                throw new IOException(
                    "Connection closed inside HTTP response line"
                );
            }

            if (cr) {
                if (value != '\n') {
                    throw new IOException(
                        "Invalid HTTP response line ending"
                    );
                }

                return buffer.toString(
                    StandardCharsets.ISO_8859_1
                );
            }

            if (value == '\r') {
                cr = true;
            } else {
                buffer.write(value);
            }
        }
    }

    /**
     * Represents one connection between a test client and a worker.
     *
     * @param socket
     *     the client side of the connection.
     * @param worker
     *     the worker thread.
     */
    protected record Connection(
        Socket socket,
        Thread worker
    ) implements AutoCloseable {

        /**
         * Closes the client connection and waits for the worker to terminate.
         *
         * @throws Exception
         *     if closing or waiting fails.
         */
        @Override
        public void close() throws Exception {
            socket.close();

            worker.join(TIMEOUT);

            if (worker.isAlive()) {
                throw new IllegalStateException(
                    "Worker did not terminate"
                );
            }
        }
    }

    /**
     * Represents an HTTP response received by a test client.
     *
     * @param statusLine
     *     the HTTP status line.
     * @param headers
     *     the response headers, with lower-case names.
     * @param body
     *     the response body.
     */
    protected record TestResponse(
        String statusLine,
        Map<String, String> headers,
        byte[] body
    ) {

        /**
         * Returns the response body as UTF-8 text.
         *
         * @return
         *     the response body.
         */
        protected String text() {
            return new String(
                body,
                StandardCharsets.UTF_8
            );
        }

        /**
         * Returns a response header.
         *
         * @param name
         *     the header name.
         * @return
         *     the header value, or {@code null} if absent.
         */
        protected String header(final String name) {
            return headers.get(
                name.toLowerCase(Locale.ENGLISH)
            );
        }
    }
}
