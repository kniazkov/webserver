/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the {@link Response} interface and its default builder implementation.
 */
final class ResponseTest {

    /**
     * Tests building a response with default values.
     */
    @Test
    void buildsResponseWithDefaultValues() {
        final Response response = Response.builder().build();

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            response.getContentType()
        );
        assertEquals(Map.of(), response.getHeaders());
        assertArrayEquals(new byte[0], response.getData());
    }

    /**
     * Tests building a plain text response.
     */
    @Test
    void buildsPlainTextResponse() {
        final Response response = Response.builder()
            .setPlainText("Hello")
            .build();

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(ContentType.TEXT_PLAIN, response.getContentType());
        assertArrayEquals(
            "Hello".getBytes(StandardCharsets.UTF_8),
            response.getData()
        );
    }

    /**
     * Tests building an HTML response.
     */
    @Test
    void buildsHtmlResponse() {
        final Response response = Response.builder()
            .setHtml("<h1>Hello</h1>")
            .build();

        assertEquals(ContentType.TEXT_HTML, response.getContentType());
        assertArrayEquals(
            "<h1>Hello</h1>".getBytes(StandardCharsets.UTF_8),
            response.getData()
        );
    }

    /**
     * Tests building a JSON response.
     */
    @Test
    void buildsJsonResponse() {
        final Response response = Response.builder()
            .setJson("{\"result\":true}")
            .build();

        assertEquals(
            ContentType.APPLICATION_JSON,
            response.getContentType()
        );
        assertArrayEquals(
            "{\"result\":true}".getBytes(StandardCharsets.UTF_8),
            response.getData()
        );
    }

    /**
     * Tests setting a custom response status.
     */
    @Test
    void setsResponseStatus() {
        final Response response = Response.builder()
            .setStatus(HttpStatus.CREATED)
            .setPlainText("Created")
            .build();

        assertEquals(HttpStatus.CREATED, response.getStatus());
    }

    /**
     * Tests setting binary response data.
     */
    @Test
    void setsBinaryData() {
        final byte[] data = new byte[] {1, 2, 3};

        final Response response = Response.builder()
            .setContentType(ContentType.IMAGE_PNG)
            .setData(data)
            .build();

        assertEquals(ContentType.IMAGE_PNG, response.getContentType());
        assertArrayEquals(new byte[] {1, 2, 3}, response.getData());
    }

    /**
     * Tests that response data is copied when passed to the builder.
     */
    @Test
    void copiesDataProvidedToBuilder() {
        final byte[] data = new byte[] {1, 2, 3};

        final Response response = Response.builder()
            .setData(data)
            .build();

        data[0] = 100;

        assertArrayEquals(new byte[] {1, 2, 3}, response.getData());
    }

    /**
     * Tests that response data returned to the caller is a defensive copy.
     */
    @Test
    void copiesReturnedData() {
        final Response response = Response.builder()
            .setData(new byte[] {1, 2, 3})
            .build();

        final byte[] first = response.getData();
        first[0] = 100;

        final byte[] second = response.getData();

        assertNotSame(first, second);
        assertArrayEquals(new byte[] {1, 2, 3}, second);
    }

    /**
     * Tests adding multiple values for the same header field.
     */
    @Test
    void addsMultipleHeaderValues() {
        final Response response = Response.builder()
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Cache-Control", "no-store")
            .build();

        assertEquals(
            List.of("no-cache", "no-store"),
            response.getHeaders().get("Cache-Control")
        );
    }

    /**
     * Tests replacing all values of a header field.
     */
    @Test
    void replacesHeaderValues() {
        final Response response = Response.builder()
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Cache-Control", "no-store")
            .setHeader("Cache-Control", "private")
            .build();

        assertEquals(
            List.of("private"),
            response.getHeaders().get("Cache-Control")
        );
    }

    /**
     * Tests that response header maps cannot be modified.
     */
    @Test
    void preventsHeaderMapModification() {
        final Response response = Response.builder()
            .addHeader("Cache-Control", "no-store")
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> response.getHeaders().put(
                "X-Test",
                List.of("value")
            )
        );
    }

    /**
     * Tests that response header value lists cannot be modified.
     */
    @Test
    void preventsHeaderValueModification() {
        final Response response = Response.builder()
            .addHeader("Cache-Control", "no-store")
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> response.getHeaders()
                .get("Cache-Control")
                .add("private")
        );
    }

    /**
     * Tests that changing the builder does not modify an existing response.
     */
    @Test
    void isolatesResponseFromBuilderChanges() {
        final Response.Builder builder = Response.builder()
            .setPlainText("First")
            .addHeader("X-Test", "first")
            .setCookie("session", "first");

        final Response response = builder.build();

        builder
            .setPlainText("Second")
            .setHeader("X-Test", "second")
            .setCookie("session", "second");

        assertArrayEquals(
            "First".getBytes(StandardCharsets.UTF_8),
            response.getData()
        );
        assertEquals(
            List.of("first"),
            response.getHeaders().get("X-Test")
        );
        assertEquals(
            List.of("session=first"),
            response.getHeaders().get("Set-Cookie")
        );
    }

    /**
     * Tests adding several cookies with different names.
     */
    @Test
    void addsSeveralCookies() {
        final Response response = Response.builder()
            .setCookie("session", "abc")
            .setCookie("theme", "dark")
            .build();

        assertEquals(
            List.of("session=abc", "theme=dark"),
            response.getHeaders().get("Set-Cookie")
        );
    }

    /**
     * Tests replacing a cookie with the same name.
     */
    @Test
    void replacesCookieWithSameName() {
        final Response response = Response.builder()
            .setCookie("session", "old")
            .setCookie("session", "new")
            .build();

        assertEquals(
            List.of("session=new"),
            response.getHeaders().get("Set-Cookie")
        );
    }

    /**
     * Tests that the user cannot add the Content-Length header.
     */
    @Test
    void rejectsAddingContentLengthHeader() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Response.builder()
                .addHeader("Content-Length", "999")
        );

        assertEquals(
            "Header is managed by the server: Content-Length",
            exception.getMessage()
        );
    }

    /**
     * Tests that the user cannot replace the Content-Length header.
     */
    @Test
    void rejectsSettingContentLengthHeader() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Response.builder()
                .setHeader("content-length", "999")
        );

        assertEquals(
            "Header is managed by the server: content-length",
            exception.getMessage()
        );
    }

    /**
     * Tests that the user cannot add the Content-Type header directly.
     */
    @Test
    void rejectsAddingContentTypeHeader() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Response.builder()
                .addHeader("Content-Type", "text/html")
        );

        assertEquals(
            "Header is managed by the server: Content-Type",
            exception.getMessage()
        );
    }

    /**
     * Tests that generated HTTP headers are absent from a logical response.
     */
    @Test
    void doesNotContainGeneratedHeaders() {
        final Response response = Response.builder()
            .setPlainText("Hello")
            .build();

        assertFalse(
            response.getHeaders().keySet().stream()
                .anyMatch("Content-Length"::equalsIgnoreCase)
        );
        assertFalse(
            response.getHeaders().keySet().stream()
                .anyMatch("Content-Type"::equalsIgnoreCase)
        );
    }

    /**
     * Tests rejecting a null response status.
     */
    @Test
    void rejectsNullStatus() {
        assertThrows(
            NullPointerException.class,
            () -> Response.builder().setStatus(null)
        );
    }

    /**
     * Tests rejecting a null content type.
     */
    @Test
    void rejectsNullContentType() {
        assertThrows(
            NullPointerException.class,
            () -> Response.builder().setContentType(null)
        );
    }

    /**
     * Tests rejecting null response data.
     */
    @Test
    void rejectsNullData() {
        assertThrows(
            NullPointerException.class,
            () -> Response.builder().setData(null)
        );
    }

    /**
     * Tests rejecting null plain text.
     */
    @Test
    void rejectsNullPlainText() {
        assertThrows(
            NullPointerException.class,
            () -> Response.builder().setPlainText(null)
        );
    }

    /**
     * Tests rejecting a null header name.
     */
    @Test
    void rejectsNullHeaderName() {
        assertThrows(
            NullPointerException.class,
            () -> Response.builder().addHeader(null, "value")
        );
    }

    /**
     * Tests rejecting a null header value.
     */
    @Test
    void rejectsNullHeaderValue() {
        assertThrows(
            NullPointerException.class,
            () -> Response.builder().addHeader("X-Test", null)
        );
    }

    /**
     * Tests rejecting a null cookie name.
     */
    @Test
    void rejectsNullCookieName() {
        assertThrows(
            NullPointerException.class,
            () -> Response.builder().setCookie(null, "value")
        );
    }

    /**
     * Tests rejecting a null cookie value.
     */
    @Test
    void rejectsNullCookieValue() {
        assertThrows(
            NullPointerException.class,
            () -> Response.builder().setCookie("session", null)
        );
    }

    /**
     * Tests returning the shared Not Found response.
     */
    @Test
    void returnsSharedNotFoundResponse() {
        assertSame(Response.notFound(), Response.notFound());
        assertEquals(
            HttpStatus.NOT_FOUND,
            Response.notFound().getStatus()
        );
    }

    /**
     * Tests returning the shared Internal Server Error response.
     */
    @Test
    void returnsSharedInternalServerErrorResponse() {
        assertSame(
            Response.internalServerError(),
            Response.internalServerError()
        );
        assertEquals(
            HttpStatus.INTERNAL_SERVER_ERROR,
            Response.internalServerError().getStatus()
        );
    }
}
