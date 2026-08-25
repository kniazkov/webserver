/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ResponseFactory;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link ServerLoop}.
 * <p>
 * These tests use real local TCP connections to verify accepting connections,
 * dispatching workers, limiting their number and terminating the accept loop.
 */
final class ServerLoopTest {

    /**
     * Maximum time to wait for asynchronous test operations.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    /**
     * Tests accepting and processing a client connection.
     */
    @Test
    void acceptsConnection() throws Exception {
        final Handler handler = (request, environment) ->
            environment
                .getResponseFactory()
                .fromText("Hello")
                .build();

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (TestServer server = start(options);
             Socket client = connect(server)) {

            send(
                client,
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final String response = readAll(client);

            assertTrue(response.startsWith("HTTP/1.1 200"));
            assertTrue(response.endsWith("Hello"));
        }
    }

    /**
     * Tests processing several independent client connections.
     */
    @Test
    void acceptsSeveralConnections() throws Exception {
        final AtomicInteger counter = new AtomicInteger();

        final Handler handler = (request, environment) ->
            environment
                .getResponseFactory()
                .fromText(
                    Integer.toString(counter.incrementAndGet())
                )
                .build();

        final Options options = new Options.Builder()
            .setHandler(handler)
            .build();

        try (TestServer server = start(options)) {
            for (int index = 1; index <= 3; index++) {
                try (Socket client = connect(server)) {
                    send(
                        client,
                        "GET / HTTP/1.1\r\n"
                            + "Host: localhost\r\n"
                            + "Connection: close\r\n"
                            + "\r\n"
                    );

                    final String response = readAll(client);

                    assertTrue(
                        response.endsWith(
                            Integer.toString(index)
                        )
                    );
                }
            }
        }

        assertEquals(3, counter.get());
    }

    /**
     * Tests limiting the number of concurrently active workers.
     */
    @Test
    void limitsWorkers() throws Exception {
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger maximum = new AtomicInteger();

        final CountDownLatch entered =
            new CountDownLatch(2);

        final CountDownLatch release =
            new CountDownLatch(1);

        final Handler handler = (request, environment) -> {
            final int current = active.incrementAndGet();

            maximum.accumulateAndGet(
                current,
                Math::max
            );

            entered.countDown();

            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ServerException(
                    "Handler interrupted",
                    exception
                );
            } finally {
                active.decrementAndGet();
            }

            return environment
                .getResponseFactory()
                .fromText("OK")
                .build();
        };

        final Options options = new Options.Builder()
            .setHandler(handler)
            .setMaxWorkers(2)
            .setHandlerTimeout(Duration.ofSeconds(5))
            .build();

        try (TestServer server = start(options);
             Socket first = connect(server);
             Socket second = connect(server);
             Socket third = connect(server)) {

            sendRequest(first);
            sendRequest(second);
            sendRequest(third);

            assertTrue(
                entered.await(
                    TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS
                )
            );

            /*
             * Give the server an opportunity to incorrectly start
             * the third worker if the worker limit is broken.
             */
            Thread.sleep(100);

            assertEquals(2, active.get());
            assertEquals(2, maximum.get());

            release.countDown();

            readAll(first);
            readAll(second);
            readAll(third);
        } finally {
            release.countDown();
        }

        assertEquals(2, maximum.get());
    }

    /**
     * Tests stopping the accept loop while every worker permit is occupied.
     */
    @Test
    void stopsWhileWorkersAreSaturated() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        final Handler handler = (request, environment) -> {
            entered.countDown();

            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ServerException(
                    "Handler interrupted",
                    exception
                );
            }

            return environment
                .getResponseFactory()
                .fromText("OK")
                .build();
        };

        final Options options = new Options.Builder()
            .setHandler(handler)
            .setMaxWorkers(1)
            .setHandlerTimeout(Duration.ofSeconds(5))
            .build();

        final TestServer server = start(options);

        try (
            Socket active = connect(server);
            Socket queued = connect(server)
        ) {
            sendRequest(active);

            assertTrue(
                entered.await(
                    TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS
                )
            );

            sendRequest(queued);
            server.close();

            assertFalse(server.thread().isAlive());
        } finally {
            release.countDown();
            server.close();
        }
    }

    /**
     * Tests normal termination when the listening socket is closed.
     */
    @Test
    void closesServerSocket() throws Exception {
        final Options options =
            new Options.Builder().build();

        final TestServer server = start(options);

        server.close();

        server.thread().join(TIMEOUT);

        assertFalse(server.thread().isAlive());
    }

    /**
     * Starts a server loop on an automatically selected local port.
     *
     * @param options
     *     the server options.
     * @return
     *     the test server.
     * @throws IOException
     *     if the server socket cannot be created.
     */
    private static TestServer start(final Options options)
        throws IOException {

        final ServerSocket socket =
            new ServerSocket(0);

        final Environment environment =
            new TestEnvironment(options);

        final Thread thread = Thread.startVirtualThread(
            () -> {
                try {
                    ServerLoop.run(
                        socket,
                        options,
                        environment
                    );
                } catch (ServerException exception) {
                    throw new RuntimeException(exception);
                }
            }
        );

        return new TestServer(socket, thread);
    }

    /**
     * Connects a client to the test server.
     *
     * @param server
     *     the test server.
     * @return
     *     the connected client socket.
     * @throws IOException
     *     if connecting fails.
     */
    private static Socket connect(final TestServer server)
        throws IOException {

        final Socket socket = new Socket(
            "127.0.0.1",
            server.socket().getLocalPort()
        );

        socket.setSoTimeout(
            Math.toIntExact(TIMEOUT.toMillis())
        );

        return socket;
    }

    /**
     * Sends a simple HTTP request that closes its connection.
     *
     * @param socket
     *     the client socket.
     * @throws IOException
     *     if writing fails.
     */
    private static void sendRequest(final Socket socket)
        throws IOException {

        send(
            socket,
            "GET / HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Connection: close\r\n"
                + "\r\n"
        );
    }

    /**
     * Sends data to a client connection.
     *
     * @param socket
     *     the client socket.
     * @param value
     *     the data.
     * @throws IOException
     *     if writing fails.
     */
    private static void send(
        final Socket socket,
        final String value
    ) throws IOException {

        socket.getOutputStream().write(
            value.getBytes(StandardCharsets.US_ASCII)
        );
        socket.getOutputStream().flush();
    }

    /**
     * Reads all data until the server closes the connection.
     *
     * @param socket
     *     the client socket.
     * @return
     *     the received data.
     * @throws IOException
     *     if reading fails.
     */
    private static String readAll(final Socket socket)
        throws IOException {

        return new String(
            socket.getInputStream().readAllBytes(),
            StandardCharsets.UTF_8
        );
    }

    /**
     * Represents a running test server.
     *
     * @param socket
     *     the listening socket.
     * @param thread
     *     the server loop thread.
     */
    private record TestServer(
        ServerSocket socket,
        Thread thread
    ) implements AutoCloseable {

        /**
         * Closes the listening socket and waits for the loop to terminate.
         *
         * @throws Exception
         *     if closing or waiting fails.
         */
        @Override
        public void close() throws Exception {
            socket.close();
            thread.join(TIMEOUT);

            if (thread.isAlive()) {
                throw new IllegalStateException(
                    "Server loop did not terminate"
                );
            }
        }
    }

    /**
     * Test handler environment.
     */
    private static final class TestEnvironment
        implements Environment {

        /**
         * The response factory.
         */
        private final ResponseFactory responseFactory;

        /**
         * Creates the environment.
         *
         * @param options
         *     the server options.
         */
        private TestEnvironment(final Options options) {
            responseFactory = new ResponseFactoryImpl(
                options.getErrorPage()
            );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResponseFactory getResponseFactory() {
            return responseFactory;
        }
    }
}
