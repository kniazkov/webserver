/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.Locale;
import java.util.Set;

/**
 * Defines standard HTTP header field names.
 */
public final class HttpHeaders {

    /**
     * The {@code Content-Type} header field.
     */
    public static final String CONTENT_TYPE = "Content-Type";

    /**
     * The {@code Content-Length} header field.
     */
    public static final String CONTENT_LENGTH = "Content-Length";

    /**
     * The {@code Content-Disposition} header field.
     */
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    /**
     * The {@code Cookie} header field.
     */
    public static final String COOKIE = "Cookie";

    /**
     * The {@code Set-Cookie} header field.
     */
    public static final String SET_COOKIE = "Set-Cookie";

    /**
     * The {@code Location} header field.
     */
    public static final String LOCATION = "Location";

    /**
     * The {@code Host} header field.
     */
    public static final String HOST = "Host";

    /**
     * The {@code Connection} header field.
     */
    public static final String CONNECTION = "Connection";

    /**
     * The {@code Accept} header field.
     */
    public static final String ACCEPT = "Accept";

    /**
     * The {@code User-Agent} header field.
     */
    public static final String USER_AGENT = "User-Agent";

    /**
     * The header fields managed automatically by the server.
     */
    private static final Set<String> MANAGED_HEADERS = Set.of(
        CONTENT_TYPE.toLowerCase(Locale.ENGLISH),
        CONTENT_LENGTH.toLowerCase(Locale.ENGLISH)
    );

    /**
     * Prevents instantiation of this utility class.
     */
    private HttpHeaders() {
    }

    /**
     * Returns whether the specified header field is managed automatically
     * by the server.
     *
     * @param name
     *     the header field name.
     * @return
     *     {@code true} if the header field is managed automatically;
     *     otherwise, {@code false}.
     */
    public static boolean isManaged(final String name) {
        return name != null
            && MANAGED_HEADERS.contains(name.toLowerCase(Locale.ENGLISH));
    }
}
