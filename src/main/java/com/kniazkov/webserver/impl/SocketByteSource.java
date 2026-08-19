/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Objects;

/**
 * A buffered byte source backed by a network socket.
 * <p>
 * Although the {@link ByteSource} interface exposes data one byte at a time,
 * this implementation does not perform a socket read for every requested
 * byte. Data is read from the socket input stream in blocks and stored in an
 * internal buffer. Subsequent calls to {@link #read()} consume bytes from that
 * buffer until another block has to be obtained from the socket.
 * <p>
 * Reading from the underlying socket is blocking. If no data is currently
 * available but the connection remains open, {@link #read()} waits until data
 * arrives, the connection is closed, or an I/O error occurs.
 * <p>
 * This class does not close the socket. The owner of the connection is
 * responsible for closing it.
 */
final class SocketByteSource implements ByteSource {

    /**
     * The size of the internal read buffer.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * The socket input stream.
     */
    private final InputStream input;

    /**
     * The internal read buffer.
     */
    private final byte[] buffer = new byte[BUFFER_SIZE];

    /**
     * The index of the next byte to return.
     */
    private int position;

    /**
     * The number of valid bytes currently stored in the buffer.
     */
    private int limit;

    /**
     * Creates a byte source backed by a socket.
     *
     * @param socket
     *     the socket to read from.
     * @throws ServerException
     *     if the socket input stream cannot be obtained.
     */
    SocketByteSource(final Socket socket) throws ServerException {
        Objects.requireNonNull(socket, "Socket must not be null");

        try {
            input = socket.getInputStream();
        } catch (IOException exception) {
            throw new ServerException(
                "Cannot obtain socket input stream",
                exception
            );
        }
    }

    /**
     * Reads the next byte from the socket.
     * <p>
     * If buffered data is available, no socket operation is performed.
     * Otherwise another block of data is read from the socket.
     *
     * @return
     *     the next byte in the range {@code 0..255}, or {@code -1} if the
     *     remote side has closed its output stream and no buffered data
     *     remains.
     * @throws ServerException
     *     if reading from the socket fails.
     */
    @Override
    public int read() throws ServerException {
        if (position >= limit && !fill()) {
            return -1;
        }

        return buffer[position++] & 0xff;
    }

    /**
     * Refills the internal buffer.
     *
     * @return
     *     {@code true} if at least one byte was read, or {@code false} if the
     *     end of the stream was reached.
     * @throws ServerException
     *     if reading from the socket fails.
     */
    private boolean fill() throws ServerException {
        position = 0;

        try {
            do {
                limit = input.read(buffer);
            } while (limit == 0);
        } catch (SocketTimeoutException exception) {
            throw new ConnectionTimeoutException(exception);
        } catch (IOException exception) {
            throw new ServerException(
                "Cannot read from socket",
                exception
            );
        }

        return limit >= 0;
    }
}
