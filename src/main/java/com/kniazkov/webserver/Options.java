/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import com.kniazkov.webserver.impl.DefaultErrorPage;
import com.kniazkov.webserver.impl.DefaultHandler;

import java.time.Duration;
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
     * The default maximum number of concurrently processed connections.
     */
    private static final int DEFAULT_MAX_WORKERS = 100;

    /**
     * The default socket read timeout.
     */
    private static final Duration DEFAULT_READ_TIMEOUT =
        Duration.ofSeconds(30);

    /**
     * The default request handler timeout.
     */
    private static final Duration DEFAULT_HANDLER_TIMEOUT =
        Duration.ofSeconds(30);

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
     * The maximum number of concurrently processed connections.
     */
    private final int maxWorkers;

    /**
     * The socket read timeout.
     */
    private final Duration readTimeout;

    /**
     * The maximum request handler execution time.
     */
    private final Duration handlerTimeout;

    /**
     * The error page generator.
     */
    private final ErrorPage errorPage;

    /**
     * The request handler.
     */
    private final Handler handler;

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
        maxWorkers = builder.maxWorkers;
        readTimeout = builder.readTimeout;
        handlerTimeout = builder.handlerTimeout;
        errorPage = builder.errorPage;
        handler = builder.handler;
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
     * Returns the maximum number of concurrently processed connections.
     *
     * @return
     *     the maximum number of connections.
     */
    public int getMaxWorkers() {
        return maxWorkers;
    }

    /**
     * Returns the socket read timeout.
     *
     * @return
     *     the socket read timeout.
     */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /**
     * Returns the maximum request handler execution time.
     *
     * @return
     *     the handler timeout.
     */
    public Duration getHandlerTimeout() {
        return handlerTimeout;
    }

    /**
     * Returns the error page generator.
     *
     * @return
     *     the error page generator.
     */
    public ErrorPage getErrorPage() {
        return errorPage;
    }

    /**
     * Returns the request handler.
     *
     * @return
     *     the request handler.
     */
    public Handler getHandler() {
        return handler;
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
         * The maximum number of concurrently processed connections.
         */
        private int maxWorkers = DEFAULT_MAX_WORKERS;

        /**
         * The socket read timeout.
         */
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;

        /**
         * The maximum request handler execution time.
         */
        private Duration handlerTimeout = DEFAULT_HANDLER_TIMEOUT;

        /**
         * The error page generator.
         */
        private ErrorPage errorPage = DefaultErrorPage.getInstance();

        /**
         * The request handler.
         */
        private Handler handler = DefaultHandler.getInstance();

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
         * Sets the maximum number of concurrently processed connections.
         *
         * @param value
         *     the maximum number of connections.
         * @return
         *     this builder.
         * @throws IllegalArgumentException
         *     if the value is not positive.
         */
        public Builder setMaxWorkers(final int value) {
            if (value < 1) {
                throw new IllegalArgumentException(
                    "Maximum worker count must be positive"
                );
            }

            maxWorkers = value;
            return this;
        }

        /**
         * Sets the socket read timeout.
         *
         * @param value
         *     the socket read timeout.
         * @return
         *     this builder.
         * @throws NullPointerException
         *     if the value is {@code null}.
         * @throws IllegalArgumentException
         *     if the value is zero or negative.
         */
        public Builder setReadTimeout(final Duration value) {
            validateTimeout(value, "Read timeout");
            readTimeout = value;
            return this;
        }

        /**
         * Sets the maximum request handler execution time.
         *
         * @param value
         *     the handler timeout.
         * @return
         *     this builder.
         * @throws NullPointerException
         *     if the value is {@code null}.
         * @throws IllegalArgumentException
         *     if the value is zero or negative.
         */
        public Builder setHandlerTimeout(final Duration value) {
            validateTimeout(value, "Handler timeout");
            handlerTimeout = value;
            return this;
        }

        /**
         * Sets the error page generator.
         *
         * @param value
         *     the error page generator.
         * @return
         *     this builder.
         * @throws NullPointerException
         *     if the value is {@code null}.
         */
        public Builder setErrorPage(final ErrorPage value) {
            errorPage = Objects.requireNonNull(
                value,
                "Error page must not be null"
            );
            return this;
        }

        /**
         * Sets the request handler.
         *
         * @param value
         *     the request handler.
         * @return
         *     this builder.
         * @throws NullPointerException
         *     if the value is {@code null}.
         */
        public Builder setHandler(final Handler value) {
            handler = Objects.requireNonNull(
                value,
                "Handler must not be null"
            );
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

        /**
         * Validates a timeout.
         *
         * @param value
         *     the timeout.
         * @param name
         *     the timeout name used in an error message.
         */
        private static void validateTimeout(
            final Duration value,
            final String name
        ) {
            Objects.requireNonNull(value, name);

            if (value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(
                    name + " must be positive"
                );
            }
        }
    }
}
