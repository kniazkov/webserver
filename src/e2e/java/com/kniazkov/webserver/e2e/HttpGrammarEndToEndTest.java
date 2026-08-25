/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for strict HTTP grammar and expectation handling.
 */
final class HttpGrammarEndToEndTest extends EndToEndBaseTest {

    /**
     * Tests the complete {@code 100 Continue} handshake before a request body
     * is transmitted.
     */
    @Test
    void expectContinueHandshake() throws Exception {
        startServer(
            (request, environment) -> environment
                .getResponseFactory()
                .fromBytes(request.getBody().readAllBytes())
                .build()
        );

        try (Socket socket = connect()) {
            send(
                socket,
                "POST /upload HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: 5\r\n"
                    + "Expect: 100-continue\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            assertEquals(
                "HTTP/1.1 100 Continue",
                readLine(socket.getInputStream())
            );
            assertEquals("", readLine(socket.getInputStream()));

            send(socket, "hello");

            final String response = new String(
                socket.getInputStream().readAllBytes(),
                StandardCharsets.ISO_8859_1
            );

            assertTrue(response.startsWith("HTTP/1.1 200 OK\r\n"));
            assertTrue(response.endsWith("hello"));
        }
    }

    /**
     * Tests that a signed Content-Length cannot expose a second request as a
     * smuggled message on the same connection.
     */
    @Test
    void rejectsSignedContentLengthSmuggling() throws Exception {
        startServer();

        try (Socket socket = connect()) {
            send(
                socket,
                "POST /first HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: +4\r\n"
                    + "\r\n"
                    + "data"
                    + "GET /smuggled HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "\r\n"
            );

            final String response = new String(
                socket.getInputStream().readAllBytes(),
                StandardCharsets.ISO_8859_1
            );

            assertTrue(
                response.startsWith("HTTP/1.1 400 Bad Request\r\n")
            );
            assertEquals(1, occurrences(response, "HTTP/1.1"));
            assertFalse(response.contains("smuggled"));
        }
    }

    /**
     * Tests rejection of ambiguous Content-Length plus Transfer-Encoding
     * framing.
     */
    @Test
    void rejectsAmbiguousFraming() throws Exception {
        startServer();

        try (Socket socket = connect()) {
            send(
                socket,
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: 4\r\n"
                    + "Transfer-Encoding: chunked\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
                    + "0\r\n\r\n"
            );

            assertEquals(
                "HTTP/1.1 400 Bad Request",
                readLine(socket.getInputStream())
            );
        }
    }

    /**
     * Tests the protocol status used for a syntactically valid but unsupported
     * case-sensitive method.
     */
    @Test
    void doesNotNormalizeMethodCase() throws Exception {
        startServer();

        try (Socket socket = connect()) {
            send(
                socket,
                "get / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            assertEquals(
                "HTTP/1.1 501 Not Implemented",
                readLine(socket.getInputStream())
            );
        }
    }

    /**
     * Tests the protocol status used for an unsupported expectation.
     */
    @Test
    void rejectsUnsupportedExpectation() throws Exception {
        startServer();

        try (Socket socket = connect()) {
            send(
                socket,
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Expect: diagnostic-feature\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            assertEquals(
                "HTTP/1.1 417 Expectation Failed",
                readLine(socket.getInputStream())
            );
        }
    }

    /**
     * Opens a client connection to the test server.
     *
     * @return
     *     the connected socket.
     * @throws IOException
     *     if connecting fails.
     */
    private Socket connect() throws IOException {
        final Socket socket = new Socket(
            "127.0.0.1",
            getPort()
        );
        socket.setSoTimeout(2000);
        return socket;
    }

    /**
     * Sends ISO-8859-1 request data.
     *
     * @param socket
     *     the socket.
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
            value.getBytes(StandardCharsets.ISO_8859_1)
        );
        socket.getOutputStream().flush();
    }

    /**
     * Reads one CRLF-terminated HTTP line.
     *
     * @param input
     *     the input stream.
     * @return
     *     the line without CRLF.
     * @throws IOException
     *     if the line is incomplete or invalid.
     */
    private static String readLine(final InputStream input)
        throws IOException {
        final ByteArrayOutputStream buffer =
            new ByteArrayOutputStream();

        while (true) {
            final int value = input.read();

            if (value == -1) {
                throw new IOException("Unexpected end of HTTP line");
            }

            if (value == '\r') {
                if (input.read() != '\n') {
                    throw new IOException("Invalid HTTP line ending");
                }

                return buffer.toString(
                    StandardCharsets.ISO_8859_1
                );
            }

            buffer.write(value);
        }
    }

    /**
     * Counts non-overlapping occurrences of a substring.
     *
     * @param value
     *     the complete value.
     * @param expected
     *     the substring.
     * @return
     *     the number of occurrences.
     */
    private static int occurrences(
        final String value,
        final String expected
    ) {
        int count = 0;
        int offset = 0;

        while (true) {
            offset = value.indexOf(expected, offset);

            if (offset < 0) {
                return count;
            }

            count++;
            offset += expected.length();
        }
    }
}
