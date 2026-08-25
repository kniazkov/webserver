/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a cookie sent in an HTTP response.
 */
public final class ResponseCookie {

    /**
     * The cookie name.
     */
    private final String name;

    /**
     * The cookie value.
     */
    private final String value;

    /**
     * The optional request path restriction.
     */
    private final String path;

    /**
     * The optional domain restriction.
     */
    private final String domain;

    /**
     * The optional expiration time.
     */
    private final Instant expires;

    /**
     * The optional maximum age in seconds.
     */
    private final Long maxAge;

    /**
     * Whether the cookie requires a secure connection.
     */
    private final boolean secure;

    /**
     * Whether scripts must be prevented from reading the cookie.
     */
    private final boolean httpOnly;

    /**
     * The optional cross-site request policy.
     */
    private final SameSite sameSite;

    /**
     * Creates an immutable response cookie from a builder.
     *
     * @param builder
     *     the source builder.
     */
    private ResponseCookie(final Builder builder)
        throws ServerException {

        name = builder.name;
        value = builder.value;
        path = builder.path;
        domain = builder.domain;
        expires = builder.expires;
        maxAge = builder.maxAge;
        secure = builder.secure;
        httpOnly = builder.httpOnly;
        sameSite = builder.sameSite;
        validate();
    }

    /**
     * Returns the cookie name.
     *
     * @return
     *     the cookie name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the cookie value.
     *
     * @return
     *     the cookie value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the request path restriction.
     *
     * @return
     *     the path, or an empty optional when it is unrestricted.
     */
    public Optional<String> getPath() {
        return Optional.ofNullable(path);
    }

    /**
     * Returns the domain restriction.
     *
     * @return
     *     the domain, or an empty optional when it is unrestricted.
     */
    public Optional<String> getDomain() {
        return Optional.ofNullable(domain);
    }

    /**
     * Returns the expiration time.
     *
     * @return
     *     the expiration time, or an empty optional when it is not set.
     */
    public Optional<Instant> getExpires() {
        return Optional.ofNullable(expires);
    }

    /**
     * Returns the maximum age in seconds.
     *
     * @return
     *     the maximum age, or an empty optional when it is not set.
     */
    public OptionalLong getMaxAge() {
        return maxAge == null
            ? OptionalLong.empty()
            : OptionalLong.of(maxAge);
    }

    /**
     * Returns whether the cookie requires a secure connection.
     *
     * @return
     *     {@code true} when the secure attribute is enabled.
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Returns whether scripts are prevented from reading the cookie.
     *
     * @return
     *     {@code true} when the HTTP-only attribute is enabled.
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * Returns the cross-site request policy.
     *
     * @return
     *     the policy, or an empty optional when it is not set.
     */
    public Optional<SameSite> getSameSite() {
        return Optional.ofNullable(sameSite);
    }

    /**
     * Returns the value of the corresponding {@code Set-Cookie} header.
     *
     * @return
     *     the serialized cookie.
     */
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder()
            .append(name)
            .append('=')
            .append(value);

        if (path != null) {
            result.append("; Path=").append(path);
        }

        if (domain != null) {
            result.append("; Domain=").append(domain);
        }

        if (expires != null) {
            result
                .append("; Expires=")
                .append(
                    DateTimeFormatter.RFC_1123_DATE_TIME.format(
                        expires.atZone(ZoneOffset.UTC)
                    )
                );
        }

        if (maxAge != null) {
            result.append("; Max-Age=").append(maxAge);
        }

        if (secure) {
            result.append("; Secure");
        }

        if (httpOnly) {
            result.append("; HttpOnly");
        }

        if (sameSite != null) {
            result
                .append("; SameSite=")
                .append(sameSite.getValue());
        }

        return result.toString();
    }

    /**
     * Validates this cookie.
     *
     * @throws ServerException
     *     if the cookie cannot be serialized safely.
     */
    private void validate() throws ServerException {
        if (name.isEmpty()) {
            throw new ServerException("Cookie name is missing");
        }

        for (int index = 0; index < name.length(); index++) {
            if (!isTokenCharacter(name.charAt(index))) {
                throw new ServerException(
                    "Invalid cookie name: " + name
                );
            }
        }

        for (int index = 0; index < value.length(); index++) {
            if (!isCookieOctet(value.charAt(index))) {
                throw new ServerException("Invalid cookie value");
            }
        }

        if (path != null) {
            validateAttribute("path", path);
        }

        if (domain != null) {
            validateAttribute("domain", domain);
        }

        if (sameSite == SameSite.NONE && !secure) {
            throw new ServerException(
                "SameSite=None cookie must be secure"
            );
        }
    }

    /**
     * Validates a cookie attribute.
     *
     * @param name
     *     the attribute name.
     * @param value
     *     the attribute value.
     * @throws ServerException
     *     if the attribute is invalid.
     */
    private static void validateAttribute(
        final String name,
        final String value
    ) throws ServerException {
        if (value.isEmpty()) {
            throw new ServerException(
                "Cookie " + name + " is empty"
            );
        }

        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);

            if (
                character < 0x20
                    || character >= 0x7f
                    || character == ';'
            ) {
                throw new ServerException(
                    "Invalid cookie " + name
                );
            }
        }
    }

    /**
     * Returns whether a character is valid in a cookie name.
     *
     * @param character
     *     the character.
     * @return
     *     {@code true} if the character is valid.
     */
    private static boolean isTokenCharacter(final char character) {
        return character >= '0' && character <= '9'
            || character >= 'A' && character <= 'Z'
            || character >= 'a' && character <= 'z'
            || "!#$%&'*+-.^_`|~".indexOf(character) >= 0;
    }

    /**
     * Returns whether a character is valid in an unquoted cookie value.
     *
     * @param character
     *     the character.
     * @return
     *     {@code true} if the character is valid.
     */
    private static boolean isCookieOctet(final char character) {
        return character == 0x21
            || character >= 0x23 && character <= 0x2b
            || character >= 0x2d && character <= 0x3a
            || character >= 0x3c && character <= 0x5b
            || character >= 0x5d && character <= 0x7e;
    }

    /**
     * Builds response cookies.
     */
    public static final class Builder {

        /**
         * The cookie name.
         */
        private final String name;

        /**
         * The cookie value.
         */
        private final String value;

        /**
         * The optional request path restriction.
         */
        private String path;

        /**
         * The optional domain restriction.
         */
        private String domain;

        /**
         * The optional expiration time.
         */
        private Instant expires;

        /**
         * The optional maximum age in seconds.
         */
        private Long maxAge;

        /**
         * Whether the cookie requires a secure connection.
         */
        private boolean secure;

        /**
         * Whether scripts must be prevented from reading the cookie.
         */
        private boolean httpOnly;

        /**
         * The optional cross-site request policy.
         */
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

        /**
         * Sets the request path restriction.
         *
         * @param value
         *     the path, or {@code null} to remove the restriction.
         * @return
         *     this builder.
         */
        public Builder setPath(final String value) {
            path = value;
            return this;
        }

        /**
         * Sets the domain restriction.
         *
         * @param value
         *     the domain, or {@code null} to remove the restriction.
         * @return
         *     this builder.
         */
        public Builder setDomain(final String value) {
            domain = value;
            return this;
        }

        /**
         * Sets the expiration time.
         *
         * @param value
         *     the expiration time, or {@code null} to remove it.
         * @return
         *     this builder.
         */
        public Builder setExpires(final Instant value) {
            expires = value;
            return this;
        }

        /**
         * Sets the maximum age.
         *
         * @param value
         *     the maximum age.
         * @return
         *     this builder.
         */
        public Builder setMaxAge(final Duration value) {
            maxAge = Objects.requireNonNull(
                value,
                "Maximum age must not be null"
            ).toSeconds();
            return this;
        }

        /**
         * Enables or disables the secure attribute.
         *
         * @param value
         *     whether the attribute is enabled.
         * @return
         *     this builder.
         */
        public Builder setSecure(final boolean value) {
            secure = value;
            return this;
        }

        /**
         * Enables or disables the HTTP-only attribute.
         *
         * @param value
         *     whether the attribute is enabled.
         * @return
         *     this builder.
         */
        public Builder setHttpOnly(final boolean value) {
            httpOnly = value;
            return this;
        }

        /**
         * Sets the cross-site request policy.
         *
         * @param value
         *     the policy, or {@code null} to remove it.
         * @return
         *     this builder.
         */
        public Builder setSameSite(final SameSite value) {
            sameSite = value;
            return this;
        }

        /**
         * Builds and validates an immutable response cookie.
         *
         * @return
         *     the response cookie.
         * @throws ServerException
         *     if the cookie cannot be serialized safely.
         */
        public ResponseCookie build() throws ServerException {
            return new ResponseCookie(this);
        }
    }
}
