/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Client-certificate policy for an HTTPS listener.
 */
public enum SslClientAuthentication {

    /** Do not request a client certificate. */
    DISABLED,

    /** Request a certificate but allow clients without one. */
    OPTIONAL,

    /** Require every client to present a trusted certificate. */
    REQUIRED
}
