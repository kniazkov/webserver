/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Options;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
     * Tests serving a static file whose name is percent-encoded UTF-8.
     */
    @Test
    void encodedFileName() throws Exception {
        Files.writeString(
            root.resolve("hello world-café.txt"),
            "Encoded file name",
            StandardCharsets.UTF_8
        );

        final Options options = new Options.Builder()
            .setWwwRoot(root.toString())
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /hello%20world-caf%C3%A9.txt HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 200")
            );
            assertEquals("Encoded file name", response.text());
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
                "text/html; charset=UTF-8",
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
                "text/html; charset=UTF-8",
                response.header("Content-Type")
            );
        }
    }

    /**
     * Tests that a trailing slash is accepted and preserves directory
     * semantics for static content.
     */
    @Test
    void trailingSlashDirectoryIsForbidden() throws Exception {
        Files.createDirectory(root.resolve("private"));

        final Options options = new Options.Builder()
            .setWwwRoot(root.toString())
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /private/ HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 403")
            );
        }
    }

    /**
     * Tests serving a symbolic link whose target remains inside the WWW root.
     */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void internalSymlink() throws Exception {
        Files.writeString(
            root.resolve("target.txt"),
            "Internal target",
            StandardCharsets.UTF_8
        );
        Files.createSymbolicLink(
            root.resolve("link.txt"),
            Path.of("target.txt")
        );

        final Options options = new Options.Builder()
            .setWwwRoot(root.toString())
            .build();

        try (Connection connection = connect(options)) {
            send(
                connection.socket(),
                "GET /link.txt HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
            );

            final TestResponse response =
                readResponse(connection.socket());

            assertTrue(
                response.statusLine().startsWith("HTTP/1.1 200")
            );
            assertEquals("Internal target", response.text());
        }
    }

    /**
     * Tests that a symbolic directory cannot expose a file outside the WWW
     * root.
     */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void externalSymlink() throws Exception {
        final Path outside = Files.createTempDirectory(
            "webserver-outside-"
        );

        try {
            Files.writeString(
                outside.resolve("secret.txt"),
                "Outside secret",
                StandardCharsets.UTF_8
            );
            Files.createSymbolicLink(
                root.resolve("escape"),
                outside
            );

            final Options options = new Options.Builder()
                .setWwwRoot(root.toString())
                .build();

            try (Connection connection = connect(options)) {
                send(
                    connection.socket(),
                    "GET /escape/secret.txt HTTP/1.1\r\n"
                        + "Host: localhost\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                );

                final TestResponse response =
                    readResponse(connection.socket());

                assertTrue(
                    response.statusLine().startsWith("HTTP/1.1 404")
                );
                assertFalse(
                    response.text().contains("Outside secret")
                );
            }
        } finally {
            Files.deleteIfExists(outside.resolve("secret.txt"));
            Files.deleteIfExists(outside);
        }
    }
}
