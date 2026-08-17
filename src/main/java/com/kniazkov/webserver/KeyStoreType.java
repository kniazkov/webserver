/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Supported key store types.
 */
public enum KeyStoreType {

    /**
     * PKCS #12 key store.
     */
    PKCS12("PKCS12"),

    /**
     * Java KeyStore.
     */
    JKS("JKS");

    /**
     * The Java security API name.
     */
    private final String value;

    /**
     * Creates a key store type.
     *
     * @param value
     *     the Java security API name.
     */
    KeyStoreType(final String value) {
        this.value = value;
    }

    /**
     * Returns the Java security API name.
     *
     * @return
     *     the key store type name.
     */
    public String getValue() {
        return value;
    }
}
