/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Controls whether a cookie is sent with cross-site requests.
 */
public enum SameSite {

    /**
     * Send the cookie only in a first-party context.
     */
    STRICT("Strict"),

    /**
     * Allow the cookie for safe top-level cross-site navigation.
     */
    LAX("Lax"),

    /**
     * Allow the cookie in cross-site requests.
     */
    NONE("None");

    /**
     * The value used in a Set-Cookie header.
     */
    private final String value;

    /**
     * Creates a SameSite value.
     *
     * @param value
     *     the serialized value.
     */
    SameSite(final String value) {
        this.value = value;
    }

    /**
     * Returns the serialized value.
     *
     * @return
     *     the serialized value.
     */
    public String getValue() {
        return value;
    }
}
