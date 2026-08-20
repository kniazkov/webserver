/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests validation and serialization of response cookies.
 */
final class ResponseCookieTest {

    /**
     * Tests complete Set-Cookie value generation.
     */
    @Test
    void toHeaderValue() throws Exception {
        final ResponseCookie cookie = new ResponseCookie.Builder(
            "session",
            "abc123"
        )
            .setPath("/")
            .setDomain("example.com")
            .setExpires(Instant.parse("2026-08-19T15:00:00Z"))
            .setMaxAge(Duration.ofHours(1))
            .setSecure(true)
            .setHttpOnly(true)
            .setSameSite(SameSite.NONE)
            .build();

        assertEquals(
            "session=abc123; Path=/; Domain=example.com; "
                + "Expires=Wed, 19 Aug 2026 15:00:00 GMT; "
                + "Max-Age=3600; Secure; HttpOnly; SameSite=None",
            cookie.toString()
        );
    }

    /**
     * Tests validation when a cookie is built.
     */
    @Test
    void rejectsInvalidCookie() {
        assertThrows(
            ServerException.class,
            () -> new ResponseCookie.Builder("bad name", "value")
                .build()
        );
        assertThrows(
            ServerException.class,
            () -> new ResponseCookie.Builder("name", "bad value")
                .build()
        );
        assertThrows(
            ServerException.class,
            () -> new ResponseCookie.Builder("name", "value")
                .setPath("/bad;path")
                .build()
        );
        assertThrows(
            ServerException.class,
            () -> new ResponseCookie.Builder("name", "value")
                .setSameSite(SameSite.NONE)
                .build()
        );
    }
}
