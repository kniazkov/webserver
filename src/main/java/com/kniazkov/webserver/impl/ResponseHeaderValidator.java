/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

import java.util.Locale;
import java.util.Set;

/**
 * Validates application-supplied response headers.
 */
final class ResponseHeaderValidator {

    /**
     * Headers generated or controlled by the HTTP transport layer.
     */
    private static final Set<String> SERVER_MANAGED = Set.of(
        "close",
        "connection",
        "content-length",
        "content-type",
        "keep-alive",
        "proxy-connection",
        "te",
        "trailer",
        "transfer-encoding",
        "upgrade"
    );

    /**
     * Prevents instantiation.
     */
    private ResponseHeaderValidator() {
    }

    /**
     * Validates a response header and returns its canonical name.
     *
     * @param name
     *     the header name.
     * @param value
     *     the header value.
     * @return
     *     the canonical header name.
     * @throws ServerException
     *     if the header is invalid or managed by the server.
     */
    static String validate(
        final String name,
        final String value
    ) throws ServerException {
        if (name == null || name.isEmpty()) {
            throw new ServerException(
                "Response header name is missing"
            );
        }

        if (value == null) {
            throw new ServerException(
                "Response header value is missing"
            );
        }

        for (int index = 0; index < name.length(); index++) {
            if (!Lexer.isTokenCharacter(name.charAt(index))) {
                throw new ServerException(
                    "Invalid response header name: " + name
                );
            }
        }

        if (isServerManaged(name)) {
            throw new ServerException(
                "Response header is managed by the server: " + name
            );
        }

        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);

            if (
                (character < 0x20 && character != '\t')
                    || character == 0x7f
                    || character > 0xff
            ) {
                throw new ServerException(
                    "Invalid response header value"
                );
            }
        }

        return Lexer.canonicalizeHeaderName(name);
    }

    /**
     * Returns whether a header is controlled by the server.
     *
     * @param name
     *     the header name.
     * @return
     *     whether the header is server-managed.
     */
    static boolean isServerManaged(final String name) {
        return name != null && SERVER_MANAGED.contains(
            name.toLowerCase(Locale.ENGLISH)
        );
    }
}
