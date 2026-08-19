/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the effect of the server lifecycle on the JVM lifecycle.
 */
final class ServerJvmLifecycleTest {

    /**
     * Maximum time to wait for the child JVM to stop.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * Tests that a running server keeps the JVM alive until it is stopped.
     */
    @Test
    void keepsJvmAliveUntilStopped() throws Exception {
        final String java = Path.of(
            System.getProperty("java.home"),
            "bin",
            "java"
        ).toString();

        final String classPath = System.getProperty(
            "surefire.test.class.path",
            System.getProperty("java.class.path")
        );

        final Process process = new ProcessBuilder(
            java,
            "-cp",
            classPath,
            ServerJvmProcess.class.getName()
        )
            .redirectErrorStream(true)
            .start();

        try {
            final BufferedReader output = new BufferedReader(
                new InputStreamReader(
                    process.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

            assertEquals(
                ServerJvmProcess.STARTED,
                output.readLine()
            );

            Thread.sleep(200);

            assertTrue(
                process.isAlive(),
                "The server did not keep the JVM alive"
            );

            assertTrue(
                process.waitFor(
                    TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS
                ),
                "The JVM did not exit after Server.stop()"
            );

            assertEquals(0, process.exitValue());
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(
                    TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS
                );
            }
        }
    }
}
