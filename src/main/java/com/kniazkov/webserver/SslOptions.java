/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.io.File;
import java.util.Objects;

/**
 * Contains SSL/TLS configuration options for the web server.
 * <p>
 * Instances of this class are immutable.
 */
public final class SslOptions {

    /**
     * The default key store type.
     */
    private static final KeyStoreType DEFAULT_KEY_STORE_TYPE =
        KeyStoreType.PKCS12;

    /**
     * The default TLS protocol.
     */
    private static final SslProtocol DEFAULT_PROTOCOL = SslProtocol.TLS;

    /**
     * The key store file.
     */
    private final File keyStoreFile;

    /**
     * The key store password.
     */
    private final char[] keyStorePassword;

    /**
     * The private key password.
     */
    private final char[] keyPassword;

    /**
     * The key store type.
     */
    private final KeyStoreType keyStoreType;

    /**
     * The TLS protocol.
     */
    private final SslProtocol protocol;

    /**
     * Creates SSL options.
     *
     * @param builder
     *     the builder.
     */
    private SslOptions(final Builder builder) {
        keyStoreFile = builder.keyStoreFile;
        keyStorePassword = builder.keyStorePassword.clone();
        keyPassword = builder.keyPassword.clone();
        keyStoreType = builder.keyStoreType;
        protocol = builder.protocol;
    }

    /**
     * Returns the key store file.
     *
     * @return
     *     the key store file.
     */
    public File getKeyStoreFile() {
        return keyStoreFile;
    }

    /**
     * Returns a copy of the key store password.
     *
     * @return
     *     the key store password.
     */
    public char[] getKeyStorePassword() {
        return keyStorePassword.clone();
    }

    /**
     * Returns a copy of the private key password.
     *
     * @return
     *     the private key password.
     */
    public char[] getKeyPassword() {
        return keyPassword.clone();
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
     * Returns the TLS protocol.
     *
     * @return
     *     the TLS protocol.
     */
    public SslProtocol getProtocol() {
        return protocol;
    }

    /**
     * Builds SSL/TLS configuration options.
     */
    public static final class Builder {

        /**
         * The key store file.
         */
        private File keyStoreFile;

        /**
         * The key store password.
         */
        private char[] keyStorePassword;

        /**
         * The private key password.
         */
        private char[] keyPassword;

        /**
         * The key store type.
         */
        private KeyStoreType keyStoreType = DEFAULT_KEY_STORE_TYPE;

        /**
         * The TLS protocol.
         */
        private SslProtocol protocol = DEFAULT_PROTOCOL;

        /**
         * Creates a builder with default values.
         */
        public Builder() {
        }

        /**
         * Sets the key store file.
         *
         * @param value
         *     the key store file.
         * @return
         *     this builder.
         */
        public Builder setKeyStoreFile(final File value) {
            keyStoreFile = Objects.requireNonNull(
                value,
                "Key store file must not be null"
            );
            return this;
        }

        /**
         * Sets the key store file.
         *
         * @param value
         *     the key store file path.
         * @return
         *     this builder.
         */
        public Builder setKeyStoreFile(final String value) {
            Objects.requireNonNull(
                value,
                "Key store file must not be null"
            );

            return setKeyStoreFile(new File(value));
        }

        /**
         * Sets the key store password.
         *
         * @param value
         *     the key store password.
         * @return
         *     this builder.
         */
        public Builder setKeyStorePassword(final char[] value) {
            keyStorePassword = Objects.requireNonNull(
                value,
                "Key store password must not be null"
            ).clone();
            return this;
        }

        /**
         * Sets the key store password.
         *
         * @param value
         *     the key store password.
         * @return
         *     this builder.
         */
        public Builder setKeyStorePassword(final String value) {
            Objects.requireNonNull(
                value,
                "Key store password must not be null"
            );

            return setKeyStorePassword(value.toCharArray());
        }

        /**
         * Sets the private key password.
         *
         * @param value
         *     the private key password.
         * @return
         *     this builder.
         */
        public Builder setKeyPassword(final char[] value) {
            keyPassword = Objects.requireNonNull(
                value,
                "Key password must not be null"
            ).clone();
            return this;
        }

        /**
         * Sets the private key password.
         *
         * @param value
         *     the private key password.
         * @return
         *     this builder.
         */
        public Builder setKeyPassword(final String value) {
            Objects.requireNonNull(
                value,
                "Key password must not be null"
            );

            return setKeyPassword(value.toCharArray());
        }

        /**
         * Sets both the key store password and the private key password.
         *
         * @param value
         *     the password.
         * @return
         *     this builder.
         */
        public Builder setPassword(final char[] value) {
            Objects.requireNonNull(
                value,
                "Password must not be null"
            );

            setKeyStorePassword(value);
            setKeyPassword(value);

            return this;
        }

        /**
         * Sets both the key store password and the private key password.
         *
         * @param value
         *     the password.
         * @return
         *     this builder.
         */
        public Builder setPassword(final String value) {
            Objects.requireNonNull(
                value,
                "Password must not be null"
            );

            return setPassword(value.toCharArray());
        }

        /**
         * Sets the key store type.
         *
         * @param value
         *     the key store type.
         * @return
         *     this builder.
         */
        public Builder setKeyStoreType(final KeyStoreType value) {
            keyStoreType = Objects.requireNonNull(
                value,
                "Key store type must not be null"
            );
            return this;
        }

        /**
         * Sets the TLS protocol.
         *
         * @param value
         *     the protocol.
         * @return
         *     this builder.
         */
        public Builder setProtocol(final SslProtocol value) {
            protocol = Objects.requireNonNull(
                value,
                "Protocol must not be null"
            );
            return this;
        }

        /**
         * Builds immutable SSL/TLS options.
         *
         * @return
         *     the SSL options.
         * @throws IllegalStateException
         *     if required SSL configuration is missing or invalid.
         */
        public SslOptions build() {
            if (keyStoreFile == null) {
                throw new IllegalStateException(
                    "Key store file is not specified"
                );
            }

            if (!keyStoreFile.exists()) {
                throw new IllegalStateException(
                    "Key store file does not exist: " + keyStoreFile
                );
            }

            if (!keyStoreFile.isFile()) {
                throw new IllegalStateException(
                    "Key store path is not a file: " + keyStoreFile
                );
            }

            if (!keyStoreFile.canRead()) {
                throw new IllegalStateException(
                    "Key store file is not readable: " + keyStoreFile
                );
            }

            if (keyStorePassword == null) {
                throw new IllegalStateException(
                    "Key store password is not specified"
                );
            }

            if (keyPassword == null) {
                keyPassword = keyStorePassword.clone();
            }

            return new SslOptions(this);
        }
    }
}
