/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Supported SSL/TLS protocols.
 */
public enum SslProtocol {

    /**
     * TLS with protocol version negotiated by the implementation.
     */
    TLS("TLS"),

    /**
     * TLS 1.2.
     */
    TLS_1_2("TLSv1.2"),

    /**
     * TLS 1.3.
     */
    TLS_1_3("TLSv1.3");

    /**
     * The Java security API name.
     */
    private final String value;

    /**
     * Creates an SSL/TLS protocol.
     *
     * @param value
     *     the Java security API name.
     */
    SslProtocol(final String value) {
        this.value = value;
    }

    /**
     * Returns the Java security API name.
     *
     * @return
     *     the protocol name.
     */
    public String getValue() {
        return value;
    }
}
