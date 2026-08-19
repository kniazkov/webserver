/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a cookie sent in an HTTP response.
 */
public final class ResponseCookie {

    private final String name;

    private final String value;

    private final String path;

    private final String domain;

    private final Instant expires;

    private final Long maxAge;

    private final boolean secure;

    private final boolean httpOnly;

    private final SameSite sameSite;

    /**
     * Creates an immutable response cookie from a builder.
     *
     * @param builder
     *     the source builder.
     */
    private ResponseCookie(final Builder builder) {
        name = builder.name;
        value = builder.value;
        path = builder.path;
        domain = builder.domain;
        expires = builder.expires;
        maxAge = builder.maxAge;
        secure = builder.secure;
        httpOnly = builder.httpOnly;
        sameSite = builder.sameSite;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Optional<String> getPath() {
        return Optional.ofNullable(path);
    }

    public Optional<String> getDomain() {
        return Optional.ofNullable(domain);
    }

    public Optional<Instant> getExpires() {
        return Optional.ofNullable(expires);
    }

    public OptionalLong getMaxAge() {
        return maxAge == null
            ? OptionalLong.empty()
            : OptionalLong.of(maxAge);
    }

    public boolean isSecure() {
        return secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public Optional<SameSite> getSameSite() {
        return Optional.ofNullable(sameSite);
    }

    /**
     * Builds response cookies.
     */
    public static final class Builder {

        private final String name;

        private final String value;

        private String path;

        private String domain;

        private Instant expires;

        private Long maxAge;

        private boolean secure;

        private boolean httpOnly;

        private SameSite sameSite;

        /**
         * Creates a cookie builder.
         *
         * @param name
         *     the cookie name.
         * @param value
         *     the cookie value.
         */
        public Builder(final String name, final String value) {
            this.name = Objects.requireNonNull(
                name,
                "Cookie name must not be null"
            );
            this.value = Objects.requireNonNull(
                value,
                "Cookie value must not be null"
            );
        }

        public Builder setPath(final String value) {
            path = value;
            return this;
        }

        public Builder setDomain(final String value) {
            domain = value;
            return this;
        }

        public Builder setExpires(final Instant value) {
            expires = value;
            return this;
        }

        public Builder setMaxAge(final Duration value) {
            maxAge = Objects.requireNonNull(
                value,
                "Maximum age must not be null"
            ).toSeconds();
            return this;
        }

        public Builder setSecure(final boolean value) {
            secure = value;
            return this;
        }

        public Builder setHttpOnly(final boolean value) {
            httpOnly = value;
            return this;
        }

        public Builder setSameSite(final SameSite value) {
            sameSite = value;
            return this;
        }

        public ResponseCookie build() {
            return new ResponseCookie(this);
        }
    }
}
