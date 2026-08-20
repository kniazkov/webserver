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

        public ResponseCookie build() throws ServerException {
            return new ResponseCookie(this);
        }
    }
}
