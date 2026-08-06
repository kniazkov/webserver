/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Represents immutable configuration options used to start the web server.
 */
public final class Options {

    /**
     * The default server port.
     */
    private static final int DEFAULT_PORT = 8000;

    /**
     * The default directory containing static files.
     */
    private static final String DEFAULT_WWW_ROOT = "www";

    /**
     * The number of bytes in one mebibyte.
     */
    private static final long MEBIBYTE = 1024L * 1024L;

    /**
     * The default maximum request size in bytes.
     */
    private static final long DEFAULT_MAX_REQUEST_SIZE = 128L * MEBIBYTE;

    /**
     * The default maximum uploaded file size in bytes.
     */
    private static final long DEFAULT_MAX_FILE_SIZE = 128L * MEBIBYTE;

    /**
     * The server port.
     */
    private final int port;

    /**
     * The directory containing static files.
     */
    private final String wwwRoot;

    /**
     * The maximum request size in bytes.
     */
    private final long maxRequestSize;

    /**
     * The maximum size of a single uploaded file in bytes.
     */
    private final long maxFileSize;

    /**
     * Creates immutable server options from the specified builder.
     *
     * @param builder
     *     the builder containing configuration values.
     */
    private Options(final Builder builder) {
        this.port = builder.port;
        this.wwwRoot = builder.wwwRoot;
        this.maxRequestSize = builder.maxRequestSize;
        this.maxFileSize = builder.maxFileSize;
    }

    /**
     * Returns the server port.
     *
     * @return
     *     the server port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the directory containing static files.
     *
     * @return
     *     the static file directory.
     */
    public String getWwwRoot() {
        return wwwRoot;
    }

    /**
     * Returns the maximum request size in bytes.
     *
     * @return
     *     the maximum request size.
     */
    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    /**
     * Returns the maximum size of a single uploaded file in bytes.
     *
     * @return
     *     the maximum uploaded file size.
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Creates a new options builder initialized with default values.
     *
     * @return
     *     the new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds immutable server configuration options.
     */
    public static final class Builder {

        /**
         * The server port.
         */
        private int port = DEFAULT_PORT;

        /**
         * The directory containing static files.
         */
        private String wwwRoot = DEFAULT_WWW_ROOT;

        /**
         * The maximum request size in bytes.
         */
        private long maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;

        /**
         * The maximum size of a single uploaded file in bytes.
         */
        private long maxFileSize = DEFAULT_MAX_FILE_SIZE;

        /**
         * Sets the server port.
         *
         * @param port
         *     the server port.
         * @return
         *     this builder.
         */
        public Builder setPort(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the directory containing static files.
         *
         * @param wwwRoot
         *     the static file directory.
         * @return
         *     this builder.
         */
        public Builder setWwwRoot(final String wwwRoot) {
            this.wwwRoot = wwwRoot;
            return this;
        }

        /**
         * Sets the maximum request size in bytes.
         *
         * @param maxRequestSize
         *     the maximum request size.
         * @return
         *     this builder.
         */
        public Builder setMaxRequestSize(final long maxRequestSize) {
            this.maxRequestSize = maxRequestSize;
            return this;
        }

        /**
         * Sets the maximum size of a single uploaded file in bytes.
         *
         * @param maxFileSize
         *     the maximum uploaded file size.
         * @return
         *     this builder.
         */
        public Builder setMaxFileSize(final long maxFileSize) {
            this.maxFileSize = maxFileSize;
            return this;
        }

        /**
         * Builds immutable server configuration options.
         *
         * @return
         *     the immutable options.
         * @throws IllegalArgumentException
         *     if any configuration value is invalid.
         */
        public Options build() {
            validatePort();
            validateWwwRoot();
            validateMaxRequestSize();
            validateMaxFileSize();
            validateSizeRelationship();

            return new Options(this);
        }

        /**
         * Validates the server port.
         *
         * @throws IllegalArgumentException
         *     if the port is outside the valid TCP port range.
         */
        private void validatePort() {
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(
                    "Port must be between 1 and 65535."
                );
            }
        }

        /**
         * Validates the static file directory.
         *
         * @throws IllegalArgumentException
         *     if the directory is {@code null} or empty.
         */
        private void validateWwwRoot() {
            if (wwwRoot == null || wwwRoot.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "WWW root must not be null or empty."
                );
            }
        }

        /**
         * Validates the maximum request size.
         *
         * @throws IllegalArgumentException
         *     if the maximum request size is not positive.
         */
        private void validateMaxRequestSize() {
            if (maxRequestSize <= 0) {
                throw new IllegalArgumentException(
                    "Maximum request size must be greater than zero."
                );
            }
        }

        /**
         * Validates the maximum uploaded file size.
         *
         * @throws IllegalArgumentException
         *     if the maximum uploaded file size is not positive.
         */
        private void validateMaxFileSize() {
            if (maxFileSize <= 0) {
                throw new IllegalArgumentException(
                    "Maximum file size must be greater than zero."
                );
            }
        }

        /**
         * Validates the relationship between request and file size limits.
         *
         * @throws IllegalArgumentException
         *     if the maximum file size exceeds the maximum request size.
         */
        private void validateSizeRelationship() {
            if (maxFileSize > maxRequestSize) {
                throw new IllegalArgumentException(
                    "Maximum file size must not exceed maximum request size."
                );
            }
        }
    }
}
