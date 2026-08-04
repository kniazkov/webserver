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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the {@link Request} class.
 */
final class RequestTest {

    /**
     * Tests building a request with all supported data.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void buildsCompleteRequest() throws ServerException {
        final RequestHeader header = createHeader();
        final UploadedFile file = new UploadedFile(
            "photo.jpg",
            "image/jpeg",
            new byte[] {1, 2, 3}
        );
        final byte[] body = "{\"name\":\"Ivan\"}".getBytes(
            StandardCharsets.UTF_8
        );

        final Request request = Request.builder()
            .setHeader(header)
            .addQueryValue("page", "1")
            .addQueryValue("page", "2")
            .addFormValue("name", "Ivan")
            .addFile("avatar", file)
            .setCookie("session", "abc123")
            .setBody(body)
            .build();

        assertEquals(header, request.getHeader());
        assertEquals(
            Map.of("page", List.of("1", "2")),
            request.getQuery()
        );
        assertEquals(
            Map.of("name", List.of("Ivan")),
            request.getForm()
        );
        assertEquals(
            Map.of("avatar", List.of(file)),
            request.getFiles()
        );
        assertEquals(
            Map.of("session", "abc123"),
            request.getCookies()
        );
        assertArrayEquals(body, request.getBody());
        assertEquals(body.length, request.getBodySize());
    }

    /**
     * Tests building a request containing only the required header.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void buildsRequestWithDefaultValues() throws ServerException {
        final Request request = Request.builder()
            .setHeader(createHeader())
            .build();

        assertEquals(Map.of(), request.getQuery());
        assertEquals(Map.of(), request.getForm());
        assertEquals(Map.of(), request.getFiles());
        assertEquals(Map.of(), request.getCookies());
        assertArrayEquals(new byte[0], request.getBody());
        assertEquals(0, request.getBodySize());
    }

    /**
     * Tests that the request header is required.
     */
    @Test
    void rejectsMissingHeader() {
        final ServerException exception = assertThrows(
            ServerException.class,
            () -> Request.builder().build()
        );

        assertEquals(
            "Request header is not specified.",
            exception.getMessage()
        );
    }

    /**
     * Tests that a null request body is rejected.
     */
    @Test
    void rejectsNullBody() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Request.builder().setBody(null)
        );

        assertEquals(
            "Request body must not be null.",
            exception.getMessage()
        );
    }

    /**
     * Tests that the request body is copied when passed to the builder.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void copiesBodyProvidedToBuilder() throws ServerException {
        final byte[] body = new byte[] {1, 2, 3};

        final Request request = Request.builder()
            .setHeader(createHeader())
            .setBody(body)
            .build();

        body[0] = 100;

        assertArrayEquals(
            new byte[] {1, 2, 3},
            request.getBody()
        );
    }

    /**
     * Tests that the returned request body is a defensive copy.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void copiesReturnedBody() throws ServerException {
        final Request request = Request.builder()
            .setHeader(createHeader())
            .setBody(new byte[] {1, 2, 3})
            .build();

        final byte[] body = request.getBody();
        body[0] = 100;

        assertArrayEquals(
            new byte[] {1, 2, 3},
            request.getBody()
        );
    }

    /**
     * Tests that modifying the builder does not modify an existing request.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void isolatesRequestFromBuilderChanges() throws ServerException {
        final Request.Builder builder = Request.builder()
            .setHeader(createHeader())
            .addQueryValue("page", "1")
            .addFormValue("name", "Ivan")
            .setCookie("theme", "dark");

        final Request request = builder.build();

        builder
            .addQueryValue("page", "2")
            .addFormValue("name", "Peter")
            .setCookie("theme", "light");

        assertEquals(
            Map.of("page", List.of("1")),
            request.getQuery()
        );
        assertEquals(
            Map.of("name", List.of("Ivan")),
            request.getForm()
        );
        assertEquals(
            Map.of("theme", "dark"),
            request.getCookies()
        );
    }

    /**
     * Tests that the query parameter map cannot be modified.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void preventsQueryMapModification() throws ServerException {
        final Request request = Request.builder()
            .setHeader(createHeader())
            .addQueryValue("page", "1")
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> request.getQuery().put("limit", List.of("10"))
        );
    }

    /**
     * Tests that query parameter lists cannot be modified.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void preventsQueryValueModification() throws ServerException {
        final Request request = Request.builder()
            .setHeader(createHeader())
            .addQueryValue("page", "1")
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> request.getQuery().get("page").add("2")
        );
    }

    /**
     * Tests that the form parameter map cannot be modified.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void preventsFormMapModification() throws ServerException {
        final Request request = Request.builder()
            .setHeader(createHeader())
            .addFormValue("name", "Ivan")
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> request.getForm().put("age", List.of("25"))
        );
    }

    /**
     * Tests that the uploaded file map cannot be modified.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void preventsFileMapModification() throws ServerException {
        final UploadedFile file = new UploadedFile(
            "photo.jpg",
            "image/jpeg",
            new byte[] {1, 2, 3}
        );

        final Request request = Request.builder()
            .setHeader(createHeader())
            .addFile("avatar", file)
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> request.getFiles().put("document", List.of(file))
        );
    }

    /**
     * Tests that uploaded file lists cannot be modified.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void preventsFileListModification() throws ServerException {
        final UploadedFile file = new UploadedFile(
            "photo.jpg",
            "image/jpeg",
            new byte[] {1, 2, 3}
        );

        final Request request = Request.builder()
            .setHeader(createHeader())
            .addFile("avatar", file)
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> request.getFiles().get("avatar").add(file)
        );
    }

    /**
     * Tests that the cookie map cannot be modified.
     *
     * @throws ServerException
     *     if the request or its header cannot be built.
     */
    @Test
    void preventsCookieMapModification() throws ServerException {
        final Request request = Request.builder()
            .setHeader(createHeader())
            .setCookie("session", "abc123")
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> request.getCookies().put("theme", "dark")
        );
    }

    /**
     * Creates a valid request header for tests.
     *
     * @return
     *     the request header.
     * @throws ServerException
     *     if the request header cannot be built.
     */
    private RequestHeader createHeader() throws ServerException {
        return RequestHeader.builder()
            .setMethod(HttpMethod.POST)
            .setTarget("/upload?page=1")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue("Host", "example.com")
            .build();
    }
}
