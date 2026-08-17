/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Options;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests static file responses produced by {@link Worker}.
 */
final class WorkerFileTest extends WorkerBaseTest {

    /**
     * Temporary WWW root.
     */
    @TempDir
    Path root;

    /**
     * Tests serving an existing static file.
     */
    @Test
    void existingFile() throws Exception {
        Files.writeString(
            root.resolve("hello.txt"),
            "Hello from file",
            StandardCharsets.UTF_8
        );

        final Options options = new Options.Builder()
            .setWwwRoot(root.toString())
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /hello.txt HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 200")
            );
            assertEquals(
                "text/plain",
                response.header("Content-Type")
            );
            assertEquals(
                "Hello from file",
                response.text()
            );
        }
    }

    /**
     * Tests requesting a static file that does not exist.
     */
    @Test
    void missingFile() throws Exception {
        final Options options = new Options.Builder()
            .setWwwRoot(root.toString())
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /missing.txt HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith(
                    "HTTP/1.1 404"
                )
            );
            assertEquals(
                "text/html",
                response.header("Content-Type")
            );
        }
    }

    /**
     * Tests requesting a directory instead of a regular static file.
     */
    @Test
    void directoryIsForbidden() throws Exception {
        Files.createDirectory(
            root.resolve("private")
        );

        final Options options = new Options.Builder()
            .setWwwRoot(root.toString())
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /private HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith(
                    "HTTP/1.1 403"
                )
            );
            assertEquals(
                "text/html",
                response.header("Content-Type")
            );
        }
    }
}
