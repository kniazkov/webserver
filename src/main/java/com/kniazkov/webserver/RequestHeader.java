/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable, parsed head of an HTTP request.
 *
 * <p>The request body and any data derived from it are deliberately not part of this class.
 * Header names are case-insensitive, and repeated header values are retained in their original
 * order.</p>
 */
public final class RequestHeader {
    private final String method;
    private final String requestTarget;
    private final int httpMajorVersion;
    private final int httpMinorVersion;
    private final Map<String, List<String>> headers;

    private RequestHeader(final Builder builder) {
        method = requirePresent(builder.method, "method");
        requestTarget = requirePresent(builder.requestTarget, "request target");
        if (builder.httpMajorVersion == null || builder.httpMinorVersion == null) {
            throw new IllegalStateException("HTTP version is required");
        }
        httpMajorVersion = builder.httpMajorVersion;
        httpMinorVersion = builder.httpMinorVersion;

        final Map<String, List<String>> copy =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        builder.headers.forEach((name, values) -> copy.put(name, List.copyOf(values)));
        headers = Collections.unmodifiableMap(copy);
    }

    /**
     * Creates an empty builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the method token exactly as received.
     *
     * @return the method token
     */
    public String method() {
        return method;
    }

    /**
     * Returns the request target exactly as received.
     *
     * @return the request target
     */
    public String requestTarget() {
        return requestTarget;
    }

    /**
     * Returns the major part of the HTTP version.
     *
     * @return the major version
     */
    public int httpMajorVersion() {
        return httpMajorVersion;
    }

    /**
     * Returns the minor part of the HTTP version.
     *
     * @return the minor version
     */
    public int httpMinorVersion() {
        return httpMinorVersion;
    }

    /**
     * Returns all header fields as a deeply unmodifiable, case-insensitive multimap.
     *
     * <p>Names are stored in lower case. Repeated values retain the order in which they were
     * added.</p>
     *
     * @return the header fields
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Returns all values associated with a header name.
     *
     * @param name the case-insensitive header name
     * @return an unmodifiable list, or an empty list if the field is absent
     */
    public List<String> headerValues(final String name) {
        final List<String> values = headers.get(normalizeHeaderName(name));
        return values == null ? List.of() : values;
    }

    /**
     * Returns the first value associated with a header name.
     *
     * @param name the case-insensitive header name
     * @return the first value, or an empty optional if the field is absent
     */
    public Optional<String> firstHeaderValue(final String name) {
        final List<String> values = headerValues(name);
        return values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    private static String requirePresent(final String value, final String name) {
        if (value == null) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static String requireToken(final String value, final String name) {
        Objects.requireNonNull(value, name);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        for (int index = 0; index < value.length(); index++) {
            if (!isTokenCharacter(value.charAt(index))) {
                throw new IllegalArgumentException(name + " is not a valid HTTP token");
            }
        }
        return value;
    }

    private static boolean isTokenCharacter(final char character) {
        return character >= '0' && character <= '9'
                || character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || "!#$%&'*+-.^_`|~".indexOf(character) >= 0;
    }

    private static String requireRequestTarget(final String value) {
        Objects.requireNonNull(value, "request target");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("request target must not be empty");
        }
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            if (character <= ' ' || character == 0x7f) {
                throw new IllegalArgumentException(
                        "request target contains whitespace or a control character");
            }
        }
        return value;
    }

    private static String normalizeHeaderName(final String name) {
        return requireToken(name, "header name").toLowerCase(Locale.ROOT);
    }

    private static String requireHeaderValue(final String value) {
        Objects.requireNonNull(value, "header value");
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            if (character < ' ' && character != '\t' || character == 0x7f) {
                throw new IllegalArgumentException("header value contains a control character");
            }
        }
        return value;
    }

    /**
     * Incrementally constructs a {@link RequestHeader}.
     */
    public static final class Builder {
        private String method;
        private String requestTarget;
        private Integer httpMajorVersion;
        private Integer httpMinorVersion;
        private final Map<String, List<String>> headers =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        private Builder() {
        }

        /**
         * Sets the method token.
         *
         * @param value the method token
         * @return this builder
         */
        public Builder method(final String value) {
            method = requireToken(value, "method");
            return this;
        }

        /**
         * Sets the request target.
         *
         * @param value the request target
         * @return this builder
         */
        public Builder requestTarget(final String value) {
            requestTarget = requireRequestTarget(value);
            return this;
        }

        /**
         * Sets the parsed HTTP version.
         *
         * @param major the non-negative major version
         * @param minor the non-negative minor version
         * @return this builder
         */
        public Builder httpVersion(final int major, final int minor) {
            if (major < 0 || minor < 0) {
                throw new IllegalArgumentException("HTTP version components must be non-negative");
            }
            httpMajorVersion = major;
            httpMinorVersion = minor;
            return this;
        }

        /**
         * Adds one header value. Calling this method repeatedly retains all values.
         *
         * @param name the case-insensitive header name
         * @param value the header value
         * @return this builder
         */
        public Builder addHeader(final String name, final String value) {
            final String normalizedName = normalizeHeaderName(name);
            final String validatedValue = requireHeaderValue(value);
            headers.computeIfAbsent(normalizedName, ignored -> new ArrayList<>())
                    .add(validatedValue);
            return this;
        }

        /**
         * Creates an immutable snapshot of this builder.
         *
         * @return the parsed request header
         * @throws IllegalStateException if a required request-line component was not set
         */
        public RequestHeader build() {
            return new RequestHeader(this);
        }
    }
}
