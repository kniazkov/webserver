/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for TLS listener configuration.
 */
final class ServerTlsTest {

    /**
     * Test key-store password.
     */
    private static final char[] PASSWORD =
        "test-password".toCharArray();

    /**
     * TLS 1.2 cipher suite supported by the test JDK.
     */
    private static final String TLS_1_2_CIPHER =
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";

    /**
     * Temporary test directory.
     */
    @TempDir
    private Path directory;

    /**
     * Tests explicit TLS protocol and cipher-suite restrictions.
     *
     * @throws Exception
     *     if the TLS connection cannot be created.
     */
    @Test
    void explicitPolicyIsApplied() throws Exception {
        final SslOptions ssl = keyStoreBuilder()
            .setEnabledProtocols(SslProtocol.TLS_1_2)
            .setCipherSuites(TLS_1_2_CIPHER)
            .build();

        final Server server = start(ssl);
        try {
            try (SSLSocket socket = connect(server)) {
                socket.setEnabledProtocols(new String[]{"TLSv1.2"});
                socket.setEnabledCipherSuites(
                    new String[]{TLS_1_2_CIPHER}
                );
                socket.startHandshake();

                assertEquals(
                    "TLSv1.2",
                    socket.getSession().getProtocol()
                );
                assertEquals(
                    TLS_1_2_CIPHER,
                    socket.getSession().getCipherSuite()
                );
            }
        } finally {
            server.stop();
        }
    }

    /**
     * Tests loading a certificate chain and PKCS #8 key from PEM files.
     *
     * @throws Exception
     *     if TLS material or the connection cannot be created.
     */
    @Test
    void pemIdentityWorks() throws Exception {
        final PemMaterial pem = extractPemMaterial();
        final SslOptions ssl = new SslOptions.Builder()
            .setCertificateChainFile(pem.certificate().toFile())
            .setPrivateKeyFile(pem.privateKey().toFile())
            .setEnabledProtocols(SslProtocol.TLS_1_2)
            .build();

        final Server server = start(ssl);
        try {
            try (SSLSocket socket = connect(server)) {
                socket.setEnabledProtocols(new String[]{"TLSv1.2"});
                socket.startHandshake();
                assertEquals(
                    "TLSv1.2",
                    socket.getSession().getProtocol()
                );
            }
        } finally {
            server.stop();
        }
    }

    /**
     * Tests that required client authentication rejects an anonymous client.
     *
     * @throws Exception
     *     if TLS material or the connection cannot be created.
     */
    @Test
    void clientCertificateIsRequired() throws Exception {
        final PemMaterial pem = extractPemMaterial();
        final SslOptions ssl = keyStoreBuilder()
            .setTrustCertificatesFile(pem.certificate().toFile())
            .setClientAuthentication(SslClientAuthentication.REQUIRED)
            .setEnabledProtocols(SslProtocol.TLS_1_2)
            .build();

        final Server server = start(ssl);
        try {
            try (SSLSocket socket = connect(server)) {
                socket.setEnabledProtocols(new String[]{"TLSv1.2"});
                assertThrows(
                    IOException.class,
                    socket::startHandshake
                );
            }
        } finally {
            server.stop();
        }
    }

    /**
     * Tests a successful handshake with a trusted client certificate.
     *
     * @throws Exception
     *     if TLS material or the connection cannot be created.
     */
    @Test
    void trustedClientCertificateWorks() throws Exception {
        final PemMaterial pem = extractPemMaterial();
        final SslOptions ssl = keyStoreBuilder()
            .setTrustCertificatesFile(pem.certificate().toFile())
            .setClientAuthentication(SslClientAuthentication.REQUIRED)
            .setEnabledProtocols(SslProtocol.TLS_1_2)
            .build();

        final Server server = start(ssl);
        try {
            try (SSLSocket socket = connectWithCertificate(server)) {
                socket.setEnabledProtocols(new String[]{"TLSv1.2"});
                socket.startHandshake();
                assertEquals(
                    "TLSv1.2",
                    socket.getSession().getProtocol()
                );
            }
        } finally {
            server.stop();
        }
    }

    /**
     * Tests that an unsupported cipher suite fails with a clear error.
     */
    @Test
    void invalidCipherIsRejectedAtStartup() {
        final SslOptions ssl = keyStoreBuilder()
            .setCipherSuites("TLS_NOT_A_REAL_CIPHER_SUITE")
            .build();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> start(ssl)
        );

        assertTrue(
            exception.getMessage().startsWith(
                "Invalid TLS listener policy:"
            )
        );
    }

    /**
     * Tests the explicit error for an unsupported encrypted PEM key.
     *
     * @throws Exception
     *     if test PEM material cannot be created.
     */
    @Test
    void encryptedPemKeyIsRejectedClearly() throws Exception {
        final PemMaterial pem = extractPemMaterial();
        Files.writeString(
            pem.privateKey(),
            "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
                + "AA==\n"
                + "-----END ENCRYPTED PRIVATE KEY-----\n",
            StandardCharsets.US_ASCII
        );
        final SslOptions ssl = new SslOptions.Builder()
            .setCertificateChainFile(pem.certificate().toFile())
            .setPrivateKeyFile(pem.privateKey().toFile())
            .build();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> start(ssl)
        );

        assertTrue(
            exception.getMessage().contains(
                "Encrypted PEM private keys are not supported"
            )
        );
    }

    /**
     * Creates a builder configured with the test PKCS #12 key store.
     */
    private SslOptions.Builder keyStoreBuilder() {
        return new SslOptions.Builder()
            .setKeyStoreFile(keyStoreFile())
            .setPassword(PASSWORD);
    }

    /**
     * Starts a TLS server on an automatically selected loopback port.
     */
    private static Server start(final SslOptions ssl)
        throws ServerException {
        return Server.start(
            new Options.Builder()
                .setPort(0)
                .setBindAddress(InetAddress.getLoopbackAddress())
                .setSslOptions(ssl)
                .build()
        );
    }

    /**
     * Creates a client socket that trusts the test server certificate.
     */
    private static SSLSocket connect(final Server server)
        throws Exception {
        return connect(server, null);
    }

    /**
     * Creates a client socket that presents the test certificate.
     */
    private SSLSocket connectWithCertificate(final Server server)
        throws Exception {
        final KeyStore store = loadKeyStore();
        final KeyManagerFactory factory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        );
        factory.init(store, PASSWORD);
        return connect(server, factory.getKeyManagers());
    }

    /**
     * Creates a trusted test client with optional key managers.
     */
    private static SSLSocket connect(
        final Server server,
        final KeyManager[] keyManagers
    ) throws Exception {
        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(
            keyManagers,
            new TrustManager[]{new TrustAllManager()},
            new SecureRandom()
        );
        return (SSLSocket) context
            .getSocketFactory()
            .createSocket("127.0.0.1", server.getPort());
    }

    /**
     * Returns the test key-store file.
     */
    private File keyStoreFile() {
        try {
            final URL resource = Objects.requireNonNull(
                getClass().getResource("/test-certificate.p12"),
                "Test certificate is missing"
            );
            return Path.of(resource.toURI()).toFile();
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Cannot locate test certificate",
                exception
            );
        }
    }

    /**
     * Extracts the existing test identity into PEM files.
     */
    private PemMaterial extractPemMaterial() throws Exception {
        final KeyStore store = loadKeyStore();

        final Enumeration<String> aliases = store.aliases();
        if (!aliases.hasMoreElements()) {
            throw new IllegalStateException("Test key store is empty");
        }
        final String alias = aliases.nextElement();
        final Key key = store.getKey(alias, PASSWORD);
        final Certificate[] chain = store.getCertificateChain(alias);

        final StringBuilder certificates = new StringBuilder();
        for (Certificate certificate : chain) {
            certificates.append(pem("CERTIFICATE", certificate.getEncoded()));
        }

        final Path certificateFile = Files.writeString(
            directory.resolve("server-chain.pem"),
            certificates,
            StandardCharsets.US_ASCII
        );
        final Path privateKeyFile = Files.writeString(
            directory.resolve("server-key.pem"),
            pem("PRIVATE KEY", key.getEncoded()),
            StandardCharsets.US_ASCII
        );
        return new PemMaterial(certificateFile, privateKeyFile);
    }

    /**
     * Loads the PKCS #12 test identity.
     */
    private KeyStore loadKeyStore() throws Exception {
        final KeyStore store = KeyStore.getInstance("PKCS12");
        try (FileInputStream input = new FileInputStream(keyStoreFile())) {
            store.load(input, PASSWORD);
        }
        return store;
    }

    /**
     * Encodes binary data as a PEM block.
     */
    private static String pem(final String type, final byte[] value) {
        return "-----BEGIN " + type + "-----\n"
            + Base64.getMimeEncoder(
                64,
                new byte[]{'\n'}
            ).encodeToString(value)
            + "\n-----END " + type + "-----\n";
    }

    /**
     * Paths of extracted test PEM material.
     */
    private record PemMaterial(Path certificate, Path privateKey) {
    }

    /**
     * Trust manager used only by local TLS tests.
     */
    private static final class TrustAllManager implements X509TrustManager {

        /**
         * Accepts every client certificate.
         */
        @Override
        public void checkClientTrusted(
            final X509Certificate[] chain,
            final String authenticationType
        ) {
        }

        /**
         * Accepts every server certificate.
         */
        @Override
        public void checkServerTrusted(
            final X509Certificate[] chain,
            final String authenticationType
        ) {
        }

        /**
         * Returns no pre-approved issuers.
         */
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
