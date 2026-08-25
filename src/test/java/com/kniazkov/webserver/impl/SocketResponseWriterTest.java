/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests bounded writes to client sockets.
 */
final class SocketResponseWriterTest {

    /**
     * Maximum time to wait for an asynchronous test operation.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    /**
     * Tests a response that is written before its deadline.
     */
    @Test
    void completesWrite() throws Exception {
        final TestSocket socket = new TestSocket(
            new ByteArrayOutputStream()
        );
        final byte[] response = {1, 2, 3};

        new SocketResponseWriter(
            socket,
            Duration.ofSeconds(1)
        ).write(response);

        Thread.sleep(Duration.ofMillis(50));

        assertFalse(socket.isClosed());
        assertArrayEquals(
            response,
            socket.data()
        );
    }

    /**
     * Tests closing a connection whose output stream stops making progress.
     */
    @Test
    void closesBlockedConnection() throws Exception {
        final BlockingOutputStream output =
            new BlockingOutputStream();
        final TestSocket socket = new TestSocket(output);
        final SocketResponseWriter writer =
            new SocketResponseWriter(
                socket,
                Duration.ofMillis(50)
            );

        final FutureTask<Void> task = new FutureTask<>(
            () -> {
                assertThrows(
                    SocketException.class,
                    () -> writer.write(new byte[] {1})
                );
                return null;
            }
        );

        Thread.startVirtualThread(task);

        assertTrue(
            output.awaitWrite(TIMEOUT)
        );

        task.get(
            TIMEOUT.toMillis(),
            TimeUnit.MILLISECONDS
        );

        assertTrue(socket.isClosed());
    }

    /**
     * A socket backed by a test output stream.
     */
    private static final class TestSocket extends Socket {

        /**
         * The test output stream.
         */
        private final OutputStream output;

        /**
         * Whether this socket was closed.
         */
        private volatile boolean closed;

        /**
         * Creates a test socket.
         *
         * @param output
         *     the output stream.
         */
        private TestSocket(final OutputStream output) {
            this.output = output;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            closed = true;
            output.close();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isClosed() {
            return closed;
        }

        /**
         * Returns bytes written to the socket.
         *
         * @return
         *     the written bytes.
         */
        private byte[] data() {
            return ((ByteArrayOutputStream) output).toByteArray();
        }
    }

    /**
     * An output stream that remains blocked until it is closed.
     */
    private static final class BlockingOutputStream
        extends OutputStream {

        /**
         * Signals that a write has started.
         */
        private final CountDownLatch writing =
            new CountDownLatch(1);

        /**
         * Signals that the stream was closed.
         */
        private final CountDownLatch closed =
            new CountDownLatch(1);

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int value) throws IOException {
            writing.countDown();

            try {
                closed.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException(
                    "Write interrupted",
                    exception
                );
            }

            throw new SocketException("Socket closed");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            closed.countDown();
        }

        /**
         * Waits until a write starts.
         *
         * @param timeout
         *     the maximum wait time.
         * @return
         *     whether a write started in time.
         * @throws InterruptedException
         *     if the wait is interrupted.
         */
        private boolean awaitWrite(final Duration timeout)
            throws InterruptedException {
            return writing.await(
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }
    }
}
