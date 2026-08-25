/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests successful construction of HTTP request headers.
 */
final class RequestHeadersBuilderTest {

    /**
     * Tests building request headers.
     */
    @Test
    void build() throws ServerException {
        final RequestHeaders headers = new RequestHeadersBuilder()
            .setMethod(HttpMethod.POST)
            .setTarget("/upload")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("content-type", "text/plain")
            .addValue("accept-encoding", "gzip")
            .build();

        assertEquals(HttpMethod.POST, headers.getMethod());
        assertEquals("/upload", headers.getTarget());
        assertEquals(HttpVersion.HTTP_1_1, headers.getVersion());

        assertEquals(
            Map.of(
                "Content-Type", List.of("text/plain"),
                "Accept-Encoding", List.of("gzip")
            ),
            headers.getValues()
        );
    }

    /**
     * Tests canonicalization of header names.
     */
    @Test
    void canonicalHeaderNames() throws ServerException {
        final RequestHeaders headers = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("content-type", "a")
            .addValue("CONTENT-LENGTH", "b")
            .addValue("uSeR-aGeNt", "c")
            .build();

        assertEquals(
            Map.of(
                "Content-Type", List.of("a"),
                "Content-Length", List.of("b"),
                "User-Agent", List.of("c")
            ),
            headers.getValues()
        );
    }

    /**
     * Tests that differently cased names refer to the same header.
     */
    @Test
    void duplicateHeaderNames() throws ServerException {
        final RequestHeaders headers = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("accept", "text/plain")
            .addValue("Accept", "text/html")
            .addValue("ACCEPT", "application/json")
            .build();

        assertEquals(
            Map.of(
                "Accept",
                List.of(
                    "text/plain",
                    "text/html",
                    "application/json"
                )
            ),
            headers.getValues()
        );
    }

    /**
     * Tests replacing header values.
     */
    @Test
    void setValue() throws ServerException {
        final RequestHeaders headers = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Accept", "text/plain")
            .addValue("accept", "text/html")
            .setValue("ACCEPT", "application/json")
            .build();

        assertEquals(
            List.of("application/json"),
            headers.getValues().get("Accept")
        );
    }

    /**
     * Tests adding values after replacing a header value.
     */
    @Test
    void addAfterSet() throws ServerException {
        final RequestHeaders headers = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Accept", "text/plain")
            .setValue("accept", "text/html")
            .addValue("ACCEPT", "application/json")
            .build();

        assertEquals(
            List.of(
                "text/html",
                "application/json"
            ),
            headers.getValues().get("Accept")
        );
    }

    /**
     * Tests building request headers without optional header fields.
     */
    @Test
    void emptyValues() throws ServerException {
        final RequestHeaders headers = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_0)
            .build();

        assertEquals(Map.of(), headers.getValues());
    }

    /**
     * Tests that built collections are immutable.
     */
    @Test
    void immutableValues() throws ServerException {
        final RequestHeaders headers = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Accept", "text/plain")
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> headers.getValues().put(
                "Content-Type",
                List.of("text/html")
            )
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> headers.getValues().get("Accept").add("text/html")
        );
    }

    /**
     * Tests that changing the builder does not affect already built headers.
     */
    @Test
    void builtObjectIsIndependent() throws ServerException {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Accept", "text/plain");

        final RequestHeaders headers = builder.build();

        builder
            .addValue("Accept", "text/html")
            .setTarget("/other");

        assertEquals("/", headers.getTarget());
        assertEquals(
            List.of("text/plain"),
            headers.getValues().get("Accept")
        );
    }
}
