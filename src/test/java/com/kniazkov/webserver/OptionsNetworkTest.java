/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests network listener options.
 */
final class OptionsNetworkTest {

    /**
     * Tests default listener configuration.
     */
    @Test
    void defaults() {
        final Options options = new Options.Builder().build();

        assertTrue(options.getBindAddress().isEmpty());
        assertEquals(50, options.getBacklog());
    }

    /**
     * Tests explicit listener configuration.
     *
     * @throws Exception
     *     if the loopback address cannot be created.
     */
    @Test
    void configured() throws Exception {
        final InetAddress address = InetAddress.getByName(
            "127.0.0.1"
        );

        final Options options = new Options.Builder()
            .setBindAddress(address)
            .setBacklog(32)
            .build();

        assertEquals(address, options.getBindAddress().orElseThrow());
        assertEquals(32, options.getBacklog());
    }

    /**
     * Tests rejection of invalid listener configuration.
     */
    @Test
    void invalid() {
        assertThrows(
            NullPointerException.class,
            () -> new Options.Builder().setBindAddress(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new Options.Builder().setBacklog(0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new Options.Builder().setBacklog(-1)
        );
    }
}
