/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Contains immutable SSL/TLS configuration for the web server.
 * <p>
 * Server credentials can be supplied either as a PKCS #12/JKS key store or
 * as an X.509 certificate chain plus an unencrypted PKCS #8 PEM private key.
 */
public final class SslOptions {

    /** Default key and trust store type. */
    private static final KeyStoreType DEFAULT_STORE_TYPE =
        KeyStoreType.PKCS12;

    /** Default TLS context protocol. */
    private static final SslProtocol DEFAULT_PROTOCOL = SslProtocol.TLS;

    /** Key store file. */
    private final File keyStoreFile;

    /** Key store password. */
    private final char[] keyStorePassword;

    /** Private key password. */
    private final char[] keyPassword;

    /** Key store type. */
    private final KeyStoreType keyStoreType;

    /** PEM certificate chain file. */
    private final File certificateChainFile;

    /** PEM private key file. */
    private final File privateKeyFile;

    /** Trust store file. */
    private final File trustStoreFile;

    /** Trust store password. */
    private final char[] trustStorePassword;

    /** Trust store type. */
    private final KeyStoreType trustStoreType;

    /** PEM trust certificate file. */
    private final File trustCertificatesFile;

    /** TLS context protocol. */
    private final SslProtocol protocol;

    /** Explicitly enabled TLS versions. */
    private final List<SslProtocol> enabledProtocols;

    /** Explicitly enabled cipher suites. */
    private final List<String> cipherSuites;

    /** Client-certificate policy. */
    private final SslClientAuthentication clientAuthentication;

    /**
     * Takes ownership of validated builder state.
     *
     * @param builder
     *     the builder.
     */
    private SslOptions(final Builder builder) {
        keyStoreFile = builder.keyStoreFile;
        keyStorePassword = builder.keyStorePassword;
        keyPassword = builder.keyPassword;
        keyStoreType = builder.keyStoreType;
        certificateChainFile = builder.certificateChainFile;
        privateKeyFile = builder.privateKeyFile;
        trustStoreFile = builder.trustStoreFile;
        trustStorePassword = builder.trustStorePassword;
        trustStoreType = builder.trustStoreType;
        trustCertificatesFile = builder.trustCertificatesFile;
        protocol = builder.protocol;
        enabledProtocols = builder.enabledProtocols;
        cipherSuites = builder.cipherSuites;
        clientAuthentication = builder.clientAuthentication;
    }

    /**
     * Returns the key store file.
     *
     * @return
     *     the key store file.
     * @throws IllegalStateException
     *     if this configuration uses a PEM identity.
     */
    public File getKeyStoreFile() {
        return configured(
            keyStoreFile,
            "Key store identity is not configured"
        );
    }

    /**
     * Returns a copy of the key store password.
     *
     * @return
     *     the key store password.
     * @throws IllegalStateException
     *     if this configuration uses a PEM identity.
     */
    public char[] getKeyStorePassword() {
        return configured(
            keyStorePassword,
            "Key store identity is not configured"
        ).clone();
    }

    /**
     * Returns a copy of the private key password.
     *
     * @return
     *     the private key password.
     * @throws IllegalStateException
     *     if this configuration uses a PEM identity.
     */
    public char[] getKeyPassword() {
        return configured(
            keyPassword,
            "Key store identity is not configured"
        ).clone();
    }

    /**
     * Returns the key store type.
     *
     * @return
     *     the key store type.
     */
    public KeyStoreType getKeyStoreType() {
        return keyStoreType;
    }

    /**
     * Returns the PEM certificate chain file.
     *
     * @return
     *     the file, or an empty optional for a key-store identity.
     */
    public Optional<File> getCertificateChainFile() {
        return Optional.ofNullable(certificateChainFile);
    }

    /**
     * Returns the PEM private key file.
     *
     * @return
     *     the file, or an empty optional for a key-store identity.
     */
    public Optional<File> getPrivateKeyFile() {
        return Optional.ofNullable(privateKeyFile);
    }

    /**
     * Returns the client trust-store file.
     *
     * @return
     *     the file, if configured.
     */
    public Optional<File> getTrustStoreFile() {
        return Optional.ofNullable(trustStoreFile);
    }

    /**
     * Returns a copy of the client trust-store password.
     *
     * @return
     *     the password, if configured.
     */
    public Optional<char[]> getTrustStorePassword() {
        return copy(trustStorePassword);
    }

    /**
     * Returns the client trust-store type.
     *
     * @return
     *     the trust-store type.
     */
    public KeyStoreType getTrustStoreType() {
        return trustStoreType;
    }

    /**
     * Returns the PEM client trust-certificate file.
     *
     * @return
     *     the file, if configured.
     */
    public Optional<File> getTrustCertificatesFile() {
        return Optional.ofNullable(trustCertificatesFile);
    }

    /**
     * Returns the TLS context protocol.
     *
     * @return
     *     the context protocol.
     */
    public SslProtocol getProtocol() {
        return protocol;
    }

    /**
     * Returns explicitly enabled TLS versions.
     *
     * @return
     *     the versions, or an empty list for provider defaults.
     */
    public List<SslProtocol> getEnabledProtocols() {
        return enabledProtocols;
    }

    /**
     * Returns explicitly enabled cipher suites.
     *
     * @return
     *     the suites, or an empty list for provider defaults.
     */
    public List<String> getCipherSuites() {
        return cipherSuites;
    }

    /**
     * Returns the client-certificate policy.
     *
     * @return
     *     the policy.
     */
    public SslClientAuthentication getClientAuthentication() {
        return clientAuthentication;
    }

    /** Requires a configured value. */
    private static <T> T configured(final T value, final String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    /**
     * Builds SSL/TLS configuration options.
     */
    public static final class Builder {

        /** Key store file. */
        private File keyStoreFile;

        /** Key store password. */
        private char[] keyStorePassword;

        /** Private key password. */
        private char[] keyPassword;

        /** Key store type. */
        private KeyStoreType keyStoreType = DEFAULT_STORE_TYPE;

        /** PEM certificate chain file. */
        private File certificateChainFile;

        /** PEM private key file. */
        private File privateKeyFile;

        /** Trust store file. */
        private File trustStoreFile;

        /** Trust store password. */
        private char[] trustStorePassword;

        /** Trust store type. */
        private KeyStoreType trustStoreType = DEFAULT_STORE_TYPE;

        /** PEM trust certificate file. */
        private File trustCertificatesFile;

        /** TLS context protocol. */
        private SslProtocol protocol = DEFAULT_PROTOCOL;

        /** Explicitly enabled TLS versions. */
        private List<SslProtocol> enabledProtocols = List.of();

        /** Explicitly enabled cipher suites. */
        private List<String> cipherSuites = List.of();

        /** Client-certificate policy. */
        private SslClientAuthentication clientAuthentication =
            SslClientAuthentication.DISABLED;

        /** Creates a builder with default values. */
        public Builder() {
        }

        /** @param value file; @return this builder. */
        public Builder setKeyStoreFile(final File value) {
            keyStoreFile = require(value, "Key store file");
            return this;
        }

        /** @param value path; @return this builder. */
        public Builder setKeyStoreFile(final String value) {
            return setKeyStoreFile(file(value, "Key store file"));
        }

        /** @param value password; @return this builder. */
        public Builder setKeyStorePassword(final char[] value) {
            clear(keyStorePassword);
            keyStorePassword = password(value, "Key store password");
            return this;
        }

        /**
         * @param value password.
         * @return this builder.
         * @deprecated use {@link #setKeyStorePassword(char[])}.
         */
        @Deprecated(forRemoval = true)
        public Builder setKeyStorePassword(final String value) {
            return stringPassword(value, this::setKeyStorePassword);
        }

        /** @param value password; @return this builder. */
        public Builder setKeyPassword(final char[] value) {
            clear(keyPassword);
            keyPassword = password(value, "Key password");
            return this;
        }

        /**
         * @param value password.
         * @return this builder.
         * @deprecated use {@link #setKeyPassword(char[])}.
         */
        @Deprecated(forRemoval = true)
        public Builder setKeyPassword(final String value) {
            return stringPassword(value, this::setKeyPassword);
        }

        /** @param value password; @return this builder. */
        public Builder setPassword(final char[] value) {
            Objects.requireNonNull(value, "Password must not be null");
            setKeyStorePassword(value);
            setKeyPassword(value);
            return this;
        }

        /**
         * @param value password.
         * @return this builder.
         * @deprecated use {@link #setPassword(char[])}.
         */
        @Deprecated(forRemoval = true)
        public Builder setPassword(final String value) {
            return stringPassword(value, this::setPassword);
        }

        /** @param value type; @return this builder. */
        public Builder setKeyStoreType(final KeyStoreType value) {
            keyStoreType = require(value, "Key store type");
            return this;
        }

        /** @param value file; @return this builder. */
        public Builder setCertificateChainFile(final File value) {
            certificateChainFile = require(value, "Certificate chain file");
            return this;
        }

        /** @param value path; @return this builder. */
        public Builder setCertificateChainFile(final String value) {
            return setCertificateChainFile(
                file(value, "Certificate chain file")
            );
        }

        /** @param value file; @return this builder. */
        public Builder setPrivateKeyFile(final File value) {
            privateKeyFile = require(value, "Private key file");
            return this;
        }

        /** @param value path; @return this builder. */
        public Builder setPrivateKeyFile(final String value) {
            return setPrivateKeyFile(file(value, "Private key file"));
        }

        /** @param value file; @return this builder. */
        public Builder setTrustStoreFile(final File value) {
            trustStoreFile = require(value, "Trust store file");
            return this;
        }

        /** @param value path; @return this builder. */
        public Builder setTrustStoreFile(final String value) {
            return setTrustStoreFile(file(value, "Trust store file"));
        }

        /** @param value password; @return this builder. */
        public Builder setTrustStorePassword(final char[] value) {
            clear(trustStorePassword);
            trustStorePassword = password(value, "Trust store password");
            return this;
        }

        /**
         * @param value password.
         * @return this builder.
         * @deprecated use {@link #setTrustStorePassword(char[])}.
         */
        @Deprecated(forRemoval = true)
        public Builder setTrustStorePassword(final String value) {
            return stringPassword(value, this::setTrustStorePassword);
        }

        /** @param value type; @return this builder. */
        public Builder setTrustStoreType(final KeyStoreType value) {
            trustStoreType = require(value, "Trust store type");
            return this;
        }

        /** @param value file; @return this builder. */
        public Builder setTrustCertificatesFile(final File value) {
            trustCertificatesFile = require(
                value,
                "Trust certificate file"
            );
            return this;
        }

        /** @param value path; @return this builder. */
        public Builder setTrustCertificatesFile(final String value) {
            return setTrustCertificatesFile(
                file(value, "Trust certificate file")
            );
        }

        /** @param value protocol; @return this builder. */
        public Builder setProtocol(final SslProtocol value) {
            protocol = require(value, "Protocol");
            return this;
        }

        /**
         * Restricts the listener to explicit TLS versions.
         *
         * @param values
         *     the versions.
         * @return
         *     this builder.
         */
        public Builder setEnabledProtocols(final SslProtocol... values) {
            Objects.requireNonNull(values, "Protocols must not be null");
            if (values.length == 0) {
                throw new IllegalArgumentException(
                    "At least one TLS protocol must be specified"
                );
            }

            enabledProtocols = Arrays.stream(values)
                .map(value -> require(value, "TLS protocol"))
                .peek(value -> {
                    if (value == SslProtocol.TLS) {
                        throw new IllegalArgumentException(
                            "Generic TLS cannot be an enabled version"
                        );
                    }
                })
                .distinct()
                .toList();
            return this;
        }

        /**
         * Restricts the listener to explicit JSSE cipher suite names.
         *
         * @param values
         *     the cipher suites.
         * @return
         *     this builder.
         */
        public Builder setCipherSuites(final String... values) {
            Objects.requireNonNull(values, "Cipher suites must not be null");
            if (values.length == 0) {
                throw new IllegalArgumentException(
                    "At least one cipher suite must be specified"
                );
            }

            cipherSuites = Arrays.stream(values)
                .map(value -> require(value, "Cipher suite"))
                .peek(value -> {
                    if (value.isBlank()) {
                        throw new IllegalArgumentException(
                            "Cipher suite must not be blank"
                        );
                    }
                })
                .distinct()
                .toList();
            return this;
        }

        /** @param value policy; @return this builder. */
        public Builder setClientAuthentication(
            final SslClientAuthentication value
        ) {
            clientAuthentication = require(
                value,
                "Client authentication"
            );
            return this;
        }

        /**
         * Builds options and consumes the password copies held by the builder.
         *
         * @return
         *     the SSL options.
         */
        public SslOptions build() {
            validateIdentity();
            validateTrust();

            final SslOptions result = new SslOptions(this);
            keyStorePassword = null;
            keyPassword = null;
            trustStorePassword = null;
            return result;
        }

        /** Validates the server identity source. */
        private void validateIdentity() {
            final boolean store = keyStoreFile != null;
            final boolean pem = certificateChainFile != null
                || privateKeyFile != null;

            if (store == pem) {
                throw new IllegalStateException(
                    "Configure exactly one TLS identity source"
                );
            }

            if (store) {
                validateFile(keyStoreFile, "Key store");
                if (keyStorePassword == null) {
                    throw new IllegalStateException(
                        "Key store password is not specified"
                    );
                }
                if (keyPassword == null) {
                    keyPassword = keyStorePassword.clone();
                }
                return;
            }

            if (certificateChainFile == null || privateKeyFile == null) {
                throw new IllegalStateException(
                    "PEM certificate chain and private key are both required"
                );
            }
            if (keyStorePassword != null || keyPassword != null) {
                throw new IllegalStateException(
                    "Passwords cannot be used with unencrypted PEM identity"
                );
            }
            validateFile(certificateChainFile, "Certificate chain");
            validateFile(privateKeyFile, "Private key");
        }

        /** Validates client trust configuration. */
        private void validateTrust() {
            if (trustStoreFile != null && trustCertificatesFile != null) {
                throw new IllegalStateException(
                    "Configure only one TLS trust source"
                );
            }
            if (trustStoreFile != null) {
                validateFile(trustStoreFile, "Trust store");
                if (trustStorePassword == null) {
                    throw new IllegalStateException(
                        "Trust store password is not specified"
                    );
                }
            } else if (trustStorePassword != null) {
                throw new IllegalStateException(
                    "Trust store password has no trust store"
                );
            }
            if (trustCertificatesFile != null) {
                validateFile(trustCertificatesFile, "Trust certificate");
            }
            if (
                clientAuthentication != SslClientAuthentication.DISABLED
                    && trustStoreFile == null
                    && trustCertificatesFile == null
            ) {
                throw new IllegalStateException(
                    "Client authentication requires explicit trust material"
                );
            }
        }

        /** Validates a TLS material file. */
        private static void validateFile(
            final File value,
            final String name
        ) {
            if (!value.exists()) {
                throw new IllegalStateException(
                    name + " file does not exist: " + value
                );
            }
            if (!value.isFile()) {
                throw new IllegalStateException(
                    name + " path is not a file: " + value
                );
            }
            if (!value.canRead()) {
                throw new IllegalStateException(
                    name + " file is not readable: " + value
                );
            }
        }

        /** Converts and promptly clears a legacy string password copy. */
        private Builder stringPassword(
            final String value,
            final Function<char[], Builder> setter
        ) {
            final char[] result = require(value, "Password").toCharArray();
            try {
                return setter.apply(result);
            } finally {
                clear(result);
            }
        }

        /** Creates a defensive password copy. */
        private static char[] password(
            final char[] value,
            final String name
        ) {
            return require(value, name).clone();
        }

        /** Converts a path to a file. */
        private static File file(final String value, final String name) {
            return new File(require(value, name));
        }

        /** Requires a non-null value. */
        private static <T> T require(final T value, final String name) {
            return Objects.requireNonNull(
                value,
                name + " must not be null"
            );
        }

        /** Clears a password copy. */
        private static void clear(final char[] value) {
            if (value != null) {
                Arrays.fill(value, '\0');
            }
        }
    }
}
