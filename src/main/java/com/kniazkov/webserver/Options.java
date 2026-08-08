/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.Objects;

/**
 * Contains configuration options for the web server.
 * <p>
 * Instances of this class are immutable.
 */
public final class Options {

    /**
     * The default server port.
     */
    private static final int DEFAULT_PORT = 8000;

    /**
     * The default root directory for static files.
     */
    private static final String DEFAULT_WWW_ROOT = "www";

    /**
     * The default maximum request size, in bytes.
     */
    private static final long DEFAULT_MAX_REQUEST_SIZE =
        128L * 1024L * 1024L;

    /**
     * The default maximum uploaded file size, in bytes.
     */
    private static final long DEFAULT_MAX_FILE_SIZE =
        128L * 1024L * 1024L;

    /**
     * The default maximum HTTP header section size, in bytes.
     */
    private static final long DEFAULT_MAX_HEADER_SIZE =
        64L * 1024L;

    /**
     * The server port.
     */
    private final int port;

    /**
     * The root directory for static files.
     */
    private final String wwwRoot;

    /**
     * The maximum request size, in bytes.
     */
    private final long maxRequestSize;

    /**
     * The maximum uploaded file size, in bytes.
     */
    private final long maxFileSize;

    /**
     * The maximum HTTP header section size, in bytes.
     */
    private final long maxHeaderSize;

    /**
     * Creates server options.
     *
     * @param builder
     *     the options builder.
     */
    private Options(final Builder builder) {
        port = builder.port;
        wwwRoot = builder.wwwRoot;
        maxHeaderSize = builder.maxHeaderSize;
        maxFileSize = builder.maxFileSize;
        maxRequestSize = Math.max(
            builder.maxRequestSize,
            Math.max(
                maxHeaderSize,
                maxFileSize
            )
        );
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
     * Returns the root directory from which static files are served.
     *
     * @return
     *     the root directory.
     */
    public String getWwwRoot() {
        return wwwRoot;
    }

    /**
     * Returns the maximum size of an HTTP request.
     *
     * @return
     *     the maximum request size, in bytes.
     */
    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    /**
     * Returns the maximum size of an uploaded file.
     *
     * @return
     *     the maximum uploaded file size, in bytes.
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Returns the maximum size of the HTTP request header section.
     * <p>
     * The limit includes the request line and all header fields.
     *
     * @return
     *     the maximum header section size, in bytes.
     */
    public long getMaxHeaderSize() {
        return maxHeaderSize;
    }

    /**
     * Builds web server options.
     */
    public static final class Builder {

        /**
         * The server port.
         */
        private int port = DEFAULT_PORT;

        /**
         * The root directory for static files.
         */
        private String wwwRoot = DEFAULT_WWW_ROOT;

        /**
         * The maximum request size, in bytes.
         */
        private long maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;

        /**
         * The maximum uploaded file size, in bytes.
         */
        private long maxFileSize = DEFAULT_MAX_FILE_SIZE;

        /**
         * The maximum HTTP header section size, in bytes.
         */
        private long maxHeaderSize = DEFAULT_MAX_HEADER_SIZE;

        /**
         * Sets the server port.
         *
         * @param value
         *     the server port.
         * @return
         *     this builder.
         * @throws IllegalArgumentException
         *     if the port is outside the range {@code 1..65535}.
         */
        public Builder setPort(final int value) {
            if (value < 1 || value > 65535) {
                throw new IllegalArgumentException(
                    "Port must be between 1 and 65535"
                );
            }

            port = value;
            return this;
        }

        /**
         * Sets the root directory from which static files are served.
         *
         * @param value
         *     the root directory.
         * @return
         *     this builder.
         * @throws NullPointerException
         *     if the value is {@code null}.
         * @throws IllegalArgumentException
         *     if the value is empty or contains only whitespace.
         */
        public Builder setWwwRoot(final String value) {
            Objects.requireNonNull(value, "wwwRoot");

            if (value.isBlank()) {
                throw new IllegalArgumentException(
                    "WWW root must not be empty"
                );
            }

            wwwRoot = value;
            return this;
        }

        /**
         * Sets the maximum size of an HTTP request.
         *
         * @param value
         *     the maximum request size, in bytes.
         * @return
         *     this builder.
         * @throws IllegalArgumentException
         *     if the value is negative.
         */
        public Builder setMaxRequestSize(final long value) {
            if (value < 0) {
                throw new IllegalArgumentException(
                    "Maximum request size must not be negative"
                );
            }

            maxRequestSize = value;
            return this;
        }

        /**
         * Sets the maximum size of an uploaded file.
         *
         * @param value
         *     the maximum uploaded file size, in bytes.
         * @return
         *     this builder.
         * @throws IllegalArgumentException
         *     if the value is negative.
         */
        public Builder setMaxFileSize(final long value) {
            if (value < 0) {
                throw new IllegalArgumentException(
                    "Maximum file size must not be negative"
                );
            }

            maxFileSize = value;
            return this;
        }

        /**
         * Sets the maximum size of the HTTP request header section.
         *
         * @param value
         *     the maximum header section size, in bytes.
         * @return
         *     this builder.
         * @throws IllegalArgumentException
         *     if the value is negative.
         */
        public Builder setMaxHeaderSize(final long value) {
            if (value < 0) {
                throw new IllegalArgumentException(
                    "Maximum header size must not be negative"
                );
            }

            maxHeaderSize = value;
            return this;
        }

        /**
         * Builds immutable web server options.
         *
         * @return
         *     the server options.
         */
        public Options build() {
            return new Options(this);
        }
    }
}
