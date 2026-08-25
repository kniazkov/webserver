/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the public server network API.
 */
final class ServerNetworkTest {

    /**
     * Tests binding an HTTP server to an explicitly selected address.
     *
     * @throws Exception
     *     if the server cannot be started or stopped.
     */
    @Test
    void bindsHttpServer() throws Exception {
        final InetAddress address = InetAddress.getByName(
            "127.0.0.1"
        );

        final Server server = Server.start(
            new Options.Builder()
                .setPort(0)
                .setBindAddress(address)
                .setBacklog(32)
                .build()
        );

        try {
            assertEquals(address, server.getBindAddress());
        } finally {
            server.stop();
        }
    }
}
