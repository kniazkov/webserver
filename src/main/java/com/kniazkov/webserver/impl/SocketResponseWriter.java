/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Writes complete responses to a client socket within a fixed deadline.
 * <p>
 * Java's blocking socket API does not provide a write timeout. A watchdog
 * therefore closes the connection if writing and flushing a response does not
 * finish in time. Closing the socket evicts the slow client and unblocks the
 * worker so its concurrency permit can be reused.
 */
final class SocketResponseWriter {

    /**
     * The client socket.
     */
    private final Socket socket;

    /**
     * The socket output stream.
     */
    private final OutputStream output;

    /**
     * The maximum time allowed for one response write.
     */
    private final Duration timeout;

    /**
     * Creates a bounded response writer.
     *
     * @param socket
     *     the client socket.
     * @param timeout
     *     the maximum time allowed for one response write.
     * @throws IOException
     *     if the socket output stream cannot be obtained.
     */
    SocketResponseWriter(
        final Socket socket,
        final Duration timeout
    ) throws IOException {
        this.socket = Objects.requireNonNull(
            socket,
            "Socket must not be null"
        );
        this.timeout = Objects.requireNonNull(
            timeout,
            "Write timeout must not be null"
        );
        output = socket.getOutputStream();
    }

    /**
     * Writes and flushes one complete serialized response.
     *
     * @param response
     *     the serialized response.
     * @throws IOException
     *     if the response cannot be written.
     */
    void write(final byte[] response) throws IOException {
        Objects.requireNonNull(
            response,
            "Response must not be null"
        );

        final AtomicBoolean finished = new AtomicBoolean();
        final Thread watchdog = Thread.startVirtualThread(
            () -> {
                try {
                    Thread.sleep(timeout);

                    if (finished.compareAndSet(false, true)) {
                        socket.close();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (IOException ignored) {
                    // The worker will observe the connection failure.
                }
            }
        );

        try {
            output.write(response);
            output.flush();
        } finally {
            if (finished.compareAndSet(false, true)) {
                watchdog.interrupt();
            }
        }
    }
}
