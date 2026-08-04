/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the {@link RequestHeader} class.
 */
final class RequestHeaderTest {

    /**
     * Tests building a request header with all supported fields.
     *
     * @throws ServerException
     *     if the request header cannot be built.
     */
    @Test
    void buildsRequestHeader() throws ServerException {
        final RequestHeader header = RequestHeader.builder()
            .setMethod(HttpMethod.GET)
            .setTarget("/index.html?name=Ivan")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Host", "example.com")
            .addValue("Accept", "text/html")
            .addValue("Accept", "application/json")
            .build();

        assertEquals(HttpMethod.GET, header.getMethod());
        assertEquals("/index.html?name=Ivan", header.getTarget());
        assertEquals(HttpVersion.HTTP_1_1, header.getVersion());
        assertEquals(
            Map.of(
                "Host", List.of("example.com"),
                "Accept", List.of("text/html", "application/json")
            ),
            header.getValues()
        );
    }

    /**
     * Tests that the order of header values is preserved.
     *
     * @throws ServerException
     *     if the request header cannot be built.
     */
    @Test
    void preservesHeaderValueOrder() throws ServerException {
        final RequestHeader header = RequestHeader.builder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Accept", "text/html")
            .addValue("Accept", "application/json")
            .addValue("Accept", "text/plain")
            .build();

        assertEquals(
            List.of("text/html", "application/json", "text/plain"),
            header.getValues().get("Accept")
        );
    }

    /**
     * Tests that an empty collection of header fields is supported.
     *
     * @throws ServerException
     *     if the request header cannot be built.
     */
    @Test
    void buildsRequestHeaderWithoutValues() throws ServerException {
        final RequestHeader header = RequestHeader.builder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_0)
            .build();

        assertEquals(Map.of(), header.getValues());
    }

    /**
     * Tests that the returned header map cannot be modified.
     *
     * @throws ServerException
     *     if the request header cannot be built.
     */
    @Test
    void preventsHeaderMapModification() throws ServerException {
        final RequestHeader header = RequestHeader.builder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Host", "example.com")
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> header.getValues().put("Accept", List.of("text/plain"))
        );
    }

    /**
     * Tests that the returned header value lists cannot be modified.
     *
     * @throws ServerException
     *     if the request header cannot be built.
     */
    @Test
    void preventsHeaderValueModification() throws ServerException {
        final RequestHeader header = RequestHeader.builder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Accept", "text/html")
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> header.getValues().get("Accept").add("application/json")
        );
    }

    /**
     * Tests that modifying the builder does not modify an already built object.
     *
     * @throws ServerException
     *     if the request header cannot be built.
     */
    @Test
    void isolatesBuiltObjectFromBuilderChanges() throws ServerException {
        final RequestHeader.Builder builder = RequestHeader.builder()
            .setMethod(HttpMethod.GET)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Accept", "text/html");

        final RequestHeader header = builder.build();

        builder.addValue("Accept", "application/json");
        builder.addValue("Host", "example.com");

        assertEquals(
            List.of("text/html"),
            header.getValues().get("Accept")
        );
        assertEquals(
            Map.of("Accept", List.of("text/html")),
            header.getValues()
        );
    }

    /**
     * Tests that the request method is required.
     */
    @Test
    void rejectsMissingMethod() {
        final ServerException exception = assertThrows(
            ServerException.class,
            () -> RequestHeader.builder()
                .setTarget("/")
                .setVersion(HttpVersion.HTTP_1_1)
                .build()
        );

        assertEquals(
            "Request method is not specified.",
            exception.getMessage()
        );
    }

    /**
     * Tests that the request target is required.
     */
    @Test
    void rejectsMissingTarget() {
        final ServerException exception = assertThrows(
            ServerException.class,
            () -> RequestHeader.builder()
                .setMethod(HttpMethod.GET)
                .setVersion(HttpVersion.HTTP_1_1)
                .build()
        );

        assertEquals(
            "Request target is not specified.",
            exception.getMessage()
        );
    }

    /**
     * Tests that the HTTP version is required.
     */
    @Test
    void rejectsMissingVersion() {
        final ServerException exception = assertThrows(
            ServerException.class,
            () -> RequestHeader.builder()
                .setMethod(HttpMethod.GET)
                .setTarget("/")
                .build()
        );

        assertEquals(
            "HTTP version is not specified.",
            exception.getMessage()
        );
    }
}
