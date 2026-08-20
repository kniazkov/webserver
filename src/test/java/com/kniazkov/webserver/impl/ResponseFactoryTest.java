/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseBuilder;
import com.kniazkov.webserver.ResponseCookie;
import com.kniazkov.webserver.ResponseFactory;
import com.kniazkov.webserver.SameSite;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the complete public response factory and builder API.
 */
final class ResponseFactoryTest {

    /**
     * The factory under test.
     */
    private final ResponseFactory factory =
        new ResponseFactoryImpl();

    /**
     * Tests a raw response with a custom status and media type.
     */
    @Test
    void rawBytes() throws Exception {
        final byte[] data = {
            0, 1, (byte) 0xff, 42
        };

        final Response response = factory
            .custom(
                HttpStatus.CREATED,
                "application/vnd.example.packet",
                data
            )
            .setHeader("X-Request-Id", "123")
            .build();

        data[0] = 99;

        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertEquals(
            "application/vnd.example.packet",
            response.getContentTypeValue()
        );
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            response.getContentType()
        );
        assertEquals(
            List.of("123"),
            response.getHeaders().get("X-Request-Id")
        );
        assertArrayEquals(
            new byte[]{0, 1, (byte) 0xff, 42},
            response.getData()
        );
    }

    /**
     * Tests that a specific factory method fixes the fundamental response
     * properties.
     */
    @Test
    void fixedJsonResponse() throws Exception {
        final Response response = factory
            .fromJson("{\"accepted\":true}")
            .build();

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(
            ContentType.APPLICATION_JSON,
            response.getContentType()
        );
        assertEquals(
            "{\"accepted\":true}",
            new String(
                response.getData(),
                StandardCharsets.UTF_8
            )
        );
    }

    /**
     * Tests complete Set-Cookie serialization.
     */
    @Test
    void cookieAttributes() throws Exception {
        final ResponseCookie cookie = new ResponseCookie.Builder(
            "session",
            "abc"
        )
            .setPath("/")
            .setDomain("example.com")
            .setExpires(Instant.parse("2026-08-19T15:00:00Z"))
            .setMaxAge(Duration.ofHours(1))
            .setSecure(true)
            .setHttpOnly(true)
            .setSameSite(SameSite.NONE)
            .build();

        final Response response = factory
            .fromText("OK")
            .setCookie(cookie)
            .build();

        assertEquals(
            List.of(
                "session=abc; Path=/; Domain=example.com; "
                    + "Expires=Wed, 19 Aug 2026 15:00:00 GMT; "
                    + "Max-Age=3600; Secure; HttpOnly; SameSite=None"
            ),
            response.getHeaders().get("Set-Cookie")
        );
    }

    /**
     * Tests redirect convenience methods.
     */
    @Test
    void redirects() throws Exception {
        final Response temporary = factory
            .redirect("/new-location")
            .build();

        final Response permanent = factory
            .redirectPermanently("https://example.com/")
            .build();

        assertEquals(HttpStatus.FOUND, temporary.getStatus());
        assertEquals(
            List.of("/new-location"),
            temporary.getHeaders().get("Location")
        );
        assertEquals(
            HttpStatus.MOVED_PERMANENTLY,
            permanent.getStatus()
        );
    }

    /**
     * Tests that internal details are hidden while explicit HTTP errors keep
     * their client-visible messages.
     */
    @Test
    void exceptionErrors() {
        final Response internal = factory.error(
            new ServerException("secret database details")
        );

        final Response conflict = factory.error(
            new ServerException(
                HttpStatus.CONFLICT,
                "Resource already exists"
            )
        );

        final String internalPage = new String(
            internal.getData(),
            StandardCharsets.UTF_8
        );

        final String conflictPage = new String(
            conflict.getData(),
            StandardCharsets.UTF_8
        );

        assertEquals(
            HttpStatus.INTERNAL_SERVER_ERROR,
            internal.getStatus()
        );
        assertFalse(internalPage.contains("secret database details"));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatus());
        assertTrue(conflictPage.contains("Resource already exists"));
    }

    /**
     * Tests body validation for statuses that prohibit response bodies.
     */
    @Test
    void noContentRejectsBody() {
        assertThrows(
            ServerException.class,
            () -> factory
                .custom(
                    HttpStatus.NO_CONTENT,
                    ContentType.TEXT_PLAIN.getValue(),
                    "Not allowed".getBytes(StandardCharsets.UTF_8)
                )
                .build()
        );
    }

    /**
     * Tests that response builders can only add response metadata.
     */
    @Test
    void builderExposesOnlyMetadata() {
        final Set<String> methods = Arrays
            .stream(ResponseBuilder.class.getDeclaredMethods())
            .map(method -> method.getName())
            .collect(Collectors.toSet());

        assertEquals(
            Set.of(
                "addHeader",
                "setHeader",
                "setCookie",
                "build"
            ),
            methods
        );
    }

    /**
     * Tests that entity headers cannot override the factory selection.
     */
    @Test
    void managedHeadersAreRejected() {
        assertThrows(
            ServerException.class,
            () -> factory
                .fromText("Text")
                .setHeader("Content-Type", "application/json")
        );

        assertThrows(
            ServerException.class,
            () -> factory
                .fromBytes(new byte[]{1, 2, 3})
                .setHeader("Content-Length", "100")
        );

        assertThrows(
            ServerException.class,
            () -> factory
                .fromBytes(new byte[]{1, 2, 3})
                .setHeader("Transfer-Encoding", "chunked")
        );
    }
}
