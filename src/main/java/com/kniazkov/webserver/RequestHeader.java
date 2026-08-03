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
 * order. Instances are deeply immutable and safe to share between threads.</p>
 */
public final class RequestHeader {
    /**
     * HTTP method token exactly as received.
     */
    private final String method;

    /**
     * Request target exactly as received.
     */
    private final String requestTarget;

    /**
     * Major component of the HTTP version.
     */
    private final int httpMajorVersion;

    /**
     * Minor component of the HTTP version.
     */
    private final int httpMinorVersion;

    /**
     * Deeply immutable, case-insensitive header multimap.
     */
    private final Map<String, List<String>> headers;

    /**
     * Creates an immutable snapshot of a fully initialized builder.
     *
     * @param builder source builder, never {@code null}
     * @throws IllegalStateException if a required request-line component is missing
     */
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
        builder.headers.forEach((final String name, final List<String> values) ->
                copy.put(name, List.copyOf(values)));
        headers = Collections.unmodifiableMap(copy);
    }

    /**
     * Creates an empty builder.
     *
     * @return a new builder, never {@code null}
     */
    public static Builder createBuilder() {
        return new Builder();
    }

    /**
     * Returns the method token exactly as received.
     *
     * @return the method token, never {@code null}
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the request target exactly as received.
     *
     * @return the request target, never {@code null}
     */
    public String getRequestTarget() {
        return requestTarget;
    }

    /**
     * Returns the major component of the HTTP version.
     *
     * @return the non-negative major version
     */
    public int getHttpMajorVersion() {
        return httpMajorVersion;
    }

    /**
     * Returns the minor component of the HTTP version.
     *
     * @return the non-negative minor version
     */
    public int getHttpMinorVersion() {
        return httpMinorVersion;
    }

    /**
     * Returns all header fields as a deeply immutable, case-insensitive multimap.
     *
     * <p>Names are stored in lower case. Repeated values retain the order in which they were
     * added.</p>
     *
     * @return the header fields, never {@code null}
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Returns all values associated with a header name.
     *
     * @param name case-insensitive header name, never {@code null}
     * @return an immutable list, empty when the field is absent and never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is not a valid HTTP token
     */
    public List<String> getHeaderValues(final String name) {
        final List<String> values = headers.get(normalizeHeaderName(name));
        return values == null ? List.of() : values;
    }

    /**
     * Returns the first value associated with a header name.
     *
     * @param name case-insensitive header name, never {@code null}
     * @return the first value, or an empty optional when the field is absent; never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is not a valid HTTP token
     */
    public Optional<String> getFirstHeaderValue(final String name) {
        final List<String> values = getHeaderValues(name);
        return values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    /**
     * Returns a required builder value or reports that it was never set.
     *
     * @param value possibly missing builder value
     * @param name value name used in the exception message, never {@code null}
     * @return the required value, never {@code null}
     * @throws IllegalStateException if {@code value} is {@code null}
     */
    private static String requirePresent(final String value, final String name) {
        if (value == null) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    /**
     * Validates and returns an HTTP token.
     *
     * @param value token to validate, never {@code null}
     * @param name token name used in exception messages, never {@code null}
     * @return the validated token, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is empty or contains an invalid character
     */
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

    /**
     * Determines whether a character is permitted in an HTTP token.
     *
     * @param character character to inspect
     * @return {@code true} when the character is permitted
     */
    private static boolean isTokenCharacter(final char character) {
        return character >= '0' && character <= '9'
                || character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || "!#$%&'*+-.^_`|~".indexOf(character) >= 0;
    }

    /**
     * Validates and returns an HTTP request target.
     *
     * @param value request target to validate, never {@code null}
     * @return the validated request target, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is empty or contains whitespace or a
     * control character
     */
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

    /**
     * Validates and normalizes a header field name for lookup.
     *
     * @param name header field name, never {@code null}
     * @return a lower-case header field name, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is not a valid HTTP token
     */
    private static String normalizeHeaderName(final String name) {
        return requireToken(name, "header name").toLowerCase(Locale.ROOT);
    }

    /**
     * Validates and returns a header field value.
     *
     * @param value header field value, never {@code null}
     * @return the validated header field value, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} contains a prohibited control character
     */
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
     * Incrementally constructs immutable {@link RequestHeader} snapshots.
     *
     * <p>A builder is mutable and not thread-safe. A built header does not observe later changes
     * to its builder.</p>
     */
    public static final class Builder {
        /**
         * HTTP method token, or {@code null} until configured.
         */
        private String method;

        /**
         * Request target, or {@code null} until configured.
         */
        private String requestTarget;

        /**
         * HTTP major version, or {@code null} until configured.
         */
        private Integer httpMajorVersion;

        /**
         * HTTP minor version, or {@code null} until configured.
         */
        private Integer httpMinorVersion;

        /**
         * Mutable header values accumulated by this builder.
         */
        private final Map<String, List<String>> headers =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        /**
         * Creates an empty builder through {@link RequestHeader#createBuilder()}.
         */
        private Builder() {
        }

        /**
         * Sets the method token.
         *
         * @param value method token, never {@code null}
         * @return this builder, never {@code null}
         * @throws NullPointerException if {@code value} is {@code null}
         * @throws IllegalArgumentException if {@code value} is not a valid HTTP token
         */
        public Builder setMethod(final String value) {
            method = requireToken(value, "method");
            return this;
        }

        /**
         * Sets the request target.
         *
         * @param value request target, never {@code null}
         * @return this builder, never {@code null}
         * @throws NullPointerException if {@code value} is {@code null}
         * @throws IllegalArgumentException if {@code value} is empty or contains whitespace or a
         * control character
         */
        public Builder setRequestTarget(final String value) {
            requestTarget = requireRequestTarget(value);
            return this;
        }

        /**
         * Sets the parsed HTTP version.
         *
         * @param major non-negative major version
         * @param minor non-negative minor version
         * @return this builder, never {@code null}
         * @throws IllegalArgumentException if either version component is negative
         */
        public Builder setHttpVersion(final int major, final int minor) {
            if (major < 0 || minor < 0) {
                throw new IllegalArgumentException("HTTP version components must be non-negative");
            }
            httpMajorVersion = major;
            httpMinorVersion = minor;
            return this;
        }

        /**
         * Adds one header value while retaining values added previously.
         *
         * @param name case-insensitive header name, never {@code null}
         * @param value header value, never {@code null}
         * @return this builder, never {@code null}
         * @throws NullPointerException if either argument is {@code null}
         * @throws IllegalArgumentException if the name or value contains a prohibited character
         */
        public Builder addHeader(final String name, final String value) {
            final String normalizedName = normalizeHeaderName(name);
            final String validatedValue = requireHeaderValue(value);
            headers.computeIfAbsent(normalizedName, (final String ignored) -> new ArrayList<>())
                    .add(validatedValue);
            return this;
        }

        /**
         * Creates an immutable snapshot of this builder.
         *
         * @return a parsed request header, never {@code null}
         * @throws IllegalStateException if a required request-line component was not set
         */
        public RequestHeader build() {
            return new RequestHeader(this);
        }
    }
}
