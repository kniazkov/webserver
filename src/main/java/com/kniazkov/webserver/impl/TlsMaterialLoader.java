/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.SslOptions;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

/**
 * Loads TLS identity and trust material from key stores or PEM files.
 */
final class TlsMaterialLoader {

    /**
     * PEM private key opening marker.
     */
    private static final String PRIVATE_KEY_BEGIN =
        "-----BEGIN PRIVATE KEY-----";

    /**
     * PEM private key closing marker.
     */
    private static final String PRIVATE_KEY_END =
        "-----END PRIVATE KEY-----";

    /**
     * Prevents instantiation.
     */
    private TlsMaterialLoader() {
    }

    /**
     * Creates key managers for the configured server identity.
     *
     * @param options
     *     the TLS options.
     * @return
     *     the key managers.
     * @throws IOException
     *     if material cannot be read.
     * @throws GeneralSecurityException
     *     if material is invalid.
     */
    static KeyManager[] loadKeyManagers(final SslOptions options)
        throws IOException, GeneralSecurityException {
        final KeyStore keyStore;
        final char[] keyPassword;

        if (options.getCertificateChainFile().isEmpty()) {
            final char[] storePassword =
                options.getKeyStorePassword();
            keyPassword = options.getKeyPassword();

            try {
                keyStore = loadStore(
                    options.getKeyStoreFile(),
                    options.getKeyStoreType().getValue(),
                    storePassword
                );
            } finally {
                clear(storePassword);
            }
        } else {
            keyPassword = new char[0];
            keyStore = loadPemIdentity(
                options.getCertificateChainFile().orElseThrow(),
                options.getPrivateKeyFile().orElseThrow(),
                keyPassword
            );
        }

        try {
            final KeyManagerFactory factory =
                KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
                );
            factory.init(keyStore, keyPassword);
            return factory.getKeyManagers();
        } finally {
            clear(keyPassword);
        }
    }

    /**
     * Creates trust managers for client-certificate verification.
     *
     * @param options
     *     the TLS options.
     * @return
     *     the trust managers, or {@code null} for provider defaults.
     * @throws IOException
     *     if material cannot be read.
     * @throws GeneralSecurityException
     *     if material is invalid.
     */
    static TrustManager[] loadTrustManagers(final SslOptions options)
        throws IOException, GeneralSecurityException {
        final KeyStore trustStore;

        if (options.getTrustStoreFile().isPresent()) {
            final char[] password = options
                .getTrustStorePassword()
                .orElseThrow();
            try {
                trustStore = loadStore(
                    options.getTrustStoreFile().orElseThrow(),
                    options.getTrustStoreType().getValue(),
                    password
                );
            } finally {
                clear(password);
            }
        } else if (options.getTrustCertificatesFile().isPresent()) {
            trustStore = loadPemTrust(
                options.getTrustCertificatesFile().orElseThrow()
            );
        } else {
            return null;
        }

        final TrustManagerFactory factory =
            TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
        factory.init(trustStore);
        return factory.getTrustManagers();
    }

    /**
     * Loads a key or trust store.
     */
    private static KeyStore loadStore(
        final File file,
        final String type,
        final char[] password
    ) throws IOException, GeneralSecurityException {
        final KeyStore result = KeyStore.getInstance(type);
        try (FileInputStream input = new FileInputStream(file)) {
            result.load(input, password);
        }
        return result;
    }

    /**
     * Loads server credentials from PEM files.
     */
    private static KeyStore loadPemIdentity(
        final File certificateFile,
        final File privateKeyFile,
        final char[] password
    ) throws IOException, GeneralSecurityException {
        final X509Certificate[] chain = certificates(certificateFile);
        final PrivateKey key = privateKey(
            privateKeyFile,
            chain[0].getPublicKey().getAlgorithm()
        );

        final KeyStore result = KeyStore.getInstance("PKCS12");
        result.load(null, null);
        result.setKeyEntry("server", key, password, chain);
        return result;
    }

    /**
     * Loads trusted client certificates from a PEM file.
     */
    private static KeyStore loadPemTrust(final File file)
        throws IOException, GeneralSecurityException {
        final X509Certificate[] certificates = certificates(file);
        final KeyStore result = KeyStore.getInstance("PKCS12");
        result.load(null, null);

        for (int index = 0; index < certificates.length; index++) {
            result.setCertificateEntry(
                "client-" + index,
                certificates[index]
            );
        }
        return result;
    }

    /**
     * Reads one or more PEM X.509 certificates.
     */
    private static X509Certificate[] certificates(final File file)
        throws IOException, GeneralSecurityException {
        final CertificateFactory factory =
            CertificateFactory.getInstance("X.509");
        final Collection<? extends Certificate> values;

        try (FileInputStream input = new FileInputStream(file)) {
            values = factory.generateCertificates(input);
        }

        if (values.isEmpty()) {
            throw new GeneralSecurityException(
                "PEM certificate file contains no certificates: " + file
            );
        }

        final X509Certificate[] result =
            new X509Certificate[values.size()];
        int index = 0;

        for (Certificate value : values) {
            if (!(value instanceof X509Certificate certificate)) {
                throw new GeneralSecurityException(
                    "PEM file contains a non-X.509 certificate: " + file
                );
            }
            result[index++] = certificate;
        }
        return result;
    }

    /**
     * Reads an unencrypted PKCS #8 PEM private key.
     */
    private static PrivateKey privateKey(
        final File file,
        final String algorithm
    ) throws IOException, GeneralSecurityException {
        final byte[] pem = Files.readAllBytes(file.toPath());
        final byte[] encryptedMarker = bytes(
            "-----BEGIN ENCRYPTED PRIVATE KEY-----"
        );
        final byte[] beginMarker = bytes(PRIVATE_KEY_BEGIN);
        final byte[] endMarker = bytes(PRIVATE_KEY_END);

        try {
            if (indexOf(pem, encryptedMarker, 0) >= 0) {
                throw new GeneralSecurityException(
                    "Encrypted PEM private keys are not supported"
                );
            }

            final int marker = indexOf(pem, beginMarker, 0);
            final int begin = marker < 0
                ? -1
                : marker + beginMarker.length;
            final int end = begin < 0
                ? -1
                : indexOf(pem, endMarker, begin);

            if (begin < 0 || end < begin) {
                throw new GeneralSecurityException(
                    "Private key must use unencrypted PKCS #8 PEM format"
                );
            }

            final byte[] encoded = Arrays.copyOfRange(pem, begin, end);
            final byte[] data;
            try {
                data = Base64.getMimeDecoder().decode(encoded);
            } catch (IllegalArgumentException exception) {
                throw new GeneralSecurityException(
                    "Invalid PEM private key encoding",
                    exception
                );
            } finally {
                Arrays.fill(encoded, (byte) 0);
            }

            try {
                return KeyFactory
                    .getInstance(algorithm)
                    .generatePrivate(new PKCS8EncodedKeySpec(data));
            } finally {
                Arrays.fill(data, (byte) 0);
            }
        } finally {
            Arrays.fill(pem, (byte) 0);
        }
    }

    /**
     * Converts a non-secret PEM marker to US-ASCII bytes.
     */
    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Finds a byte sequence without converting private material to text.
     */
    private static int indexOf(
        final byte[] value,
        final byte[] target,
        final int offset
    ) {
        final int limit = value.length - target.length;
        for (int index = offset; index <= limit; index++) {
            int targetIndex = 0;
            while (
                targetIndex < target.length
                    && value[index + targetIndex] == target[targetIndex]
            ) {
                targetIndex++;
            }
            if (targetIndex == target.length) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Clears a mutable password copy.
     */
    private static void clear(final char[] value) {
        Arrays.fill(value, '\0');
    }
}
