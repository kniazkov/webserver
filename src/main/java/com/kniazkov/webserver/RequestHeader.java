/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the header section of an HTTP request.
 * <p>
 * This class contains the request line information together with
 * all HTTP header fields.
 */
public final class RequestHeader {

    /**
     * The HTTP request method.
     */
    private final HttpMethod method;

    /**
     * The request target.
     */
    private final String target;

    /**
     * The HTTP protocol version.
     */
    private final HttpVersion version;

    /**
     * The HTTP header fields.
     */
    private final Map<String, List<String>> values;

    /**
     * Creates a new instance.
     *
     * @param builder
     *     the builder.
     */
    private RequestHeader(final Builder builder) {
        this.method = builder.method;
        this.target = builder.target;
        this.version = builder.version;

        final Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : builder.values.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.values = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns the request method.
     *
     * @return
     *     the request method.
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Returns the request target.
     *
     * @return
     *     the request target.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Returns the HTTP version.
     *
     * @return
     *     the protocol version.
     */
    public HttpVersion getVersion() {
        return version;
    }

    /**
     * Returns all header fields.
     *
     * @return
     *     an immutable map of header fields.
     */
    public Map<String, List<String>> getValues() {
        return values;
    }

    /**
     * Creates a new builder.
     *
     * @return
     *     the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds immutable request headers.
     */
    public static final class Builder {

        /**
         * The HTTP request method.
         */
        private HttpMethod method;

        /**
         * The request target.
         */
        private String target;

        /**
         * The HTTP protocol version.
         */
        private HttpVersion version;

        /**
         * The header fields.
         */
        private final Map<String, List<String>> values = new LinkedHashMap<>();

        /**
         * Sets the request method.
         *
         * @param method
         *     the request method.
         * @return
         *     this builder.
         */
        public Builder setMethod(final HttpMethod method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the request target.
         *
         * @param target
         *     the request target.
         * @return
         *     this builder.
         */
        public Builder setTarget(final String target) {
            this.target = target;
            return this;
        }

        /**
         * Sets the HTTP version.
         *
         * @param version
         *     the protocol version.
         * @return
         *     this builder.
         */
        public Builder setVersion(final HttpVersion version) {
            this.version = version;
            return this;
        }

        /**
         * Adds a header value.
         *
         * @param name
         *     the header name.
         * @param value
         *     the header value.
         * @return
         *     this builder.
         */
        public Builder addValue(final String name, final String value) {
            values.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
            return this;
        }

        /**
         * Builds the immutable request headers.
         *
         * @return
         *     the request headers.
         */
        public RequestHeader build() throws ServerException {
            if (method == null) {
                throw new ServerException("Request method is not specified.");
            }
            if (target == null) {
                throw new ServerException("Request target is not specified.");
            }
            if (version == null) {
                throw new ServerException("HTTP version is not specified.");
            }
            return new RequestHeader(this);
        }
    }
}
