/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for the public server lifecycle.
 */
final class ServerLifecycleEndToEndTest
    extends EndToEndBaseTest {

    /**
     * Tests automatic port allocation and access to the actual port.
     */
    @Test
    void automaticPort() throws Exception {
        writeFile(
            "index.html",
            "<h1>Running</h1>"
        );

        startServer();

        assertTrue(getPort() > 0);

        page.navigate(url("/index.html"));

        assertTrue(
            page.locator("h1")
                .textContent()
                .equals("Running")
        );
    }

    /**
     * Tests that stopping the server prevents new connections.
     */
    @Test
    void stop() throws Exception {
        startServer();

        final int port = getPort();

        /*
         * Verify that the port is actually accepting connections first.
         */
        try (
            Socket ignored =
                new Socket("127.0.0.1", port)
        ) {
            // Connected successfully.
        }

        stopServer();

        assertThrows(
            IOException.class,
            () -> {
                try (
                    Socket ignored =
                        new Socket("127.0.0.1", port)
                ) {
                    // Must not connect.
                }
            }
        );
    }
}
