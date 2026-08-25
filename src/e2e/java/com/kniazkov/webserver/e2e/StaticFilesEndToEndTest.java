/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for static and file responses.
 */
final class StaticFilesEndToEndTest extends EndToEndBaseTest {

    /**
     * Tests serving a static HTML page.
     */
    @Test
    void htmlPage() throws Exception {
        writeFile(
            "index.html",
            """
            <!DOCTYPE html>
            <html>
            <body>
                <h1>Hello from static file</h1>
            </body>
            </html>
            """
        );

        startServer();

        page.navigate(url("/index.html"));

        assertThat(page.locator("h1"))
            .hasText("Hello from static file");

        assertEquals(
            200,
            page.request()
                .get(url("/index.html"))
                .status()
        );
    }

    /**
     * Tests serving a static text file.
     */
    @Test
    void textFile() throws Exception {
        writeFile(
            "docs/readme.txt",
            "Static text content"
        );

        startServer();

        final String content = page.request()
            .get(url("/docs/readme.txt"))
            .text();

        assertEquals(
            "Static text content",
            content
        );
    }

    /**
     * Tests serving binary static data without modification.
     */
    @Test
    void binaryFile() throws Exception {
        final byte[] data = {
            0,
            1,
            2,
            3,
            127,
            (byte) 128,
            (byte) 254,
            (byte) 255
        };

        writeFile(
            "data.bin",
            data
        );

        startServer();

        final byte[] response = page.request()
            .get(url("/data.bin"))
            .body();

        assertEquals(data.length, response.length);

        for (int index = 0; index < data.length; index++) {
            assertEquals(
                data[index],
                response[index]
            );
        }
    }

    /**
     * Tests requesting a file that does not exist.
     */
    @Test
    void missingFile() throws Exception {
        startServer();

        final var response = page.request()
            .get(url("/missing.html"));

        assertEquals(404, response.status());
        assertTrue(
            response.headers()
                .get("content-type")
                .startsWith("text/html")
        );
    }

    /**
     * Tests requesting a directory instead of a regular file.
     */
    @Test
    void directoryIsForbidden() throws Exception {
        Files.createDirectories(
            wwwRoot.resolve("private")
        );

        startServer();

        final var response = page.request()
            .get(url("/private"));

        assertEquals(403, response.status());
    }

    /**
     * Tests returning a file explicitly from a handler.
     */
    @Test
    void handlerReturnsFile() throws Exception {
        final Path generated = Files.createTempFile(
            "webserver-generated-",
            ".txt"
        );

        try {
            Files.writeString(
                generated,
                "Generated content",
                StandardCharsets.UTF_8
            );

            final Handler handler = (request, environment) -> {
                if (
                    request.getPath()
                        .getPath()
                        .equals("/generated.txt")
                ) {
                    return environment
                        .getResponseFactory()
                        .fromFile(generated.toFile());
                }

                return environment
                    .getResponseFactory()
                    .noResponse();
            };

            startServer(handler);

            final var response = page.request()
                .get(url("/generated.txt"));

            assertEquals(200, response.status());
            assertEquals(
                "Generated content",
                response.text()
            );
            assertTrue(
                response.headers()
                    .get("content-type")
                    .startsWith("text/plain")
            );
        } finally {
            Files.deleteIfExists(generated);
        }
    }

    /**
     * Tests that an application endpoint can coexist with static files.
     */
    @Test
    void handlerAndStaticFileTogether() throws Exception {
        writeFile(
            "index.html",
            "<h1>Static</h1>"
        );

        final Handler handler = (request, environment) -> {
            if (
                request.getPath()
                    .getPath()
                    .equals("/dynamic")
            ) {
                return environment
                    .getResponseFactory()
                    .fromText("Dynamic")
                    .build();
            }

            return environment
                .getResponseFactory()
                .noResponse();
        };

        startServer(handler);

        final var dynamic = page.request()
            .get(url("/dynamic"));

        final var staticFile = page.request()
            .get(url("/index.html"));

        assertEquals(200, dynamic.status());
        assertEquals("Dynamic", dynamic.text());

        assertEquals(200, staticFile.status());
        assertTrue(
            staticFile.text().contains("Static")
        );
    }

    /**
     * Tests that the running server does not expose a file through a symbolic
     * directory that points outside the static root.
     */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void symlinkEscapeIsNotServed() throws Exception {
        final Path outside = Files.createTempDirectory(
            "webserver-e2e-outside-"
        );

        try {
            Files.writeString(
                outside.resolve("secret.txt"),
                "E2E outside secret",
                StandardCharsets.UTF_8
            );
            Files.createSymbolicLink(
                wwwRoot.resolve("escape"),
                outside
            );

            startServer();

            final var response = page.request()
                .get(url("/escape/secret.txt"));

            assertEquals(404, response.status());
            assertFalse(
                response.text().contains("E2E outside secret")
            );
        } finally {
            Files.deleteIfExists(outside.resolve("secret.txt"));
            Files.deleteIfExists(outside);
        }
    }
}
