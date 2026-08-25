/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests SSL/TLS configuration.
 */
final class SslOptionsTest {

    /** Temporary test directory. */
    @TempDir
    private Path directory;

    /**
     * Tests key-store identity and explicit listener policy.
     *
     * @throws Exception
     *     if a placeholder file cannot be created.
     */
    @Test
    void keyStorePolicy() throws Exception {
        final Path store = file("server.p12");
        final char[] password = "secret".toCharArray();

        final SslOptions options = new SslOptions.Builder()
            .setKeyStoreFile(store.toFile())
            .setPassword(password)
            .setEnabledProtocols(
                SslProtocol.TLS_1_2,
                SslProtocol.TLS_1_3,
                SslProtocol.TLS_1_2
            )
            .setCipherSuites(
                "TLS_AES_128_GCM_SHA256",
                "TLS_AES_128_GCM_SHA256"
            )
            .build();

        Arrays.fill(password, '\0');

        assertEquals(store.toFile(), options.getKeyStoreFile());
        assertArrayEquals(
            "secret".toCharArray(),
            options.getKeyStorePassword()
        );
        assertArrayEquals(
            "secret".toCharArray(),
            options.getKeyPassword()
        );
        assertEquals(
            Arrays.asList(SslProtocol.TLS_1_2, SslProtocol.TLS_1_3),
            options.getEnabledProtocols()
        );
        assertEquals(
            Arrays.asList("TLS_AES_128_GCM_SHA256"),
            options.getCipherSuites()
        );
        assertEquals(
            SslClientAuthentication.DISABLED,
            options.getClientAuthentication()
        );
    }

    /**
     * Tests that returned password arrays are defensive copies.
     *
     * @throws Exception
     *     if a placeholder file cannot be created.
     */
    @Test
    void passwordCopies() throws Exception {
        final SslOptions options = new SslOptions.Builder()
            .setKeyStoreFile(file("server.p12").toFile())
            .setPassword("secret".toCharArray())
            .build();
        final char[] first = options.getKeyPassword();
        first[0] = 'X';

        assertArrayEquals(
            "secret".toCharArray(),
            options.getKeyPassword()
        );
    }

    /**
     * Tests PEM identity and PEM trust material.
     *
     * @throws Exception
     *     if placeholder files cannot be created.
     */
    @Test
    void pemIdentityAndTrust() throws Exception {
        final Path certificate = file("server.crt");
        final Path key = file("server.key");
        final Path trust = file("clients.crt");

        final SslOptions options = new SslOptions.Builder()
            .setCertificateChainFile(certificate.toFile())
            .setPrivateKeyFile(key.toFile())
            .setTrustCertificatesFile(trust.toFile())
            .setClientAuthentication(SslClientAuthentication.REQUIRED)
            .build();

        assertEquals(
            certificate.toFile(),
            options.getCertificateChainFile().orElseThrow()
        );
        assertEquals(
            key.toFile(),
            options.getPrivateKeyFile().orElseThrow()
        );
        assertEquals(
            trust.toFile(),
            options.getTrustCertificatesFile().orElseThrow()
        );
        assertThrows(IllegalStateException.class, options::getKeyStoreFile);
        assertThrows(
            IllegalStateException.class,
            options::getKeyStorePassword
        );
    }

    /**
     * Tests validation of ambiguous or incomplete TLS material.
     *
     * @throws Exception
     *     if placeholder files cannot be created.
     */
    @Test
    void invalidMaterial() throws Exception {
        final Path store = file("server.p12");
        final Path certificate = file("server.crt");
        final Path key = file("server.key");
        final Path trustStore = file("trust.p12");
        final Path trustCertificates = file("clients.crt");

        assertThrows(
            IllegalStateException.class,
            () -> new SslOptions.Builder().build()
        );
        assertThrows(
            IllegalStateException.class,
            () -> new SslOptions.Builder()
                .setKeyStoreFile(store.toFile())
                .setPassword("secret".toCharArray())
                .setCertificateChainFile(certificate.toFile())
                .setPrivateKeyFile(key.toFile())
                .build()
        );
        assertThrows(
            IllegalStateException.class,
            () -> new SslOptions.Builder()
                .setCertificateChainFile(certificate.toFile())
                .build()
        );
        assertThrows(
            IllegalStateException.class,
            () -> new SslOptions.Builder()
                .setCertificateChainFile(certificate.toFile())
                .setPrivateKeyFile(key.toFile())
                .setClientAuthentication(
                    SslClientAuthentication.REQUIRED
                )
                .build()
        );
        assertThrows(
            IllegalStateException.class,
            () -> new SslOptions.Builder()
                .setCertificateChainFile(certificate.toFile())
                .setPrivateKeyFile(key.toFile())
                .setTrustStoreFile(trustStore.toFile())
                .setTrustStorePassword("secret".toCharArray())
                .setTrustCertificatesFile(trustCertificates.toFile())
                .build()
        );
    }

    /**
     * Tests validation of protocol and cipher restrictions.
     *
     * @throws Exception
     *     if a placeholder file cannot be created.
     */
    @Test
    void invalidPolicy() throws Exception {
        final SslOptions.Builder builder = new SslOptions.Builder()
            .setKeyStoreFile(file("server.p12").toFile())
            .setPassword("secret".toCharArray());

        assertThrows(
            IllegalArgumentException.class,
            () -> builder.setEnabledProtocols()
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.setEnabledProtocols(SslProtocol.TLS)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.setCipherSuites()
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.setCipherSuites("  ")
        );
    }

    /** Creates a readable placeholder file. */
    private Path file(final String name) throws Exception {
        return Files.writeString(directory.resolve(name), "placeholder");
    }
}
