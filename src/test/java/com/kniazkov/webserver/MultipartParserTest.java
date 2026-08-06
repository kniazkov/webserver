/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests construction and basic usage of the {@link MultipartParser} class.
 */
final class MultipartParserTest {

    /**
     * The multipart boundary used by the tests.
     */
    private static final String BOUNDARY = "TestBoundary";

    /**
     * Tests parsing an empty multipart form.
     *
     * @throws ServerException
     *     if the multipart data or request cannot be parsed.
     */
    @Test
    void parsesEmptyMultipartForm() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            Options.builder().build()
        );

        final boolean needsMoreData = parser.accept(
            createBytes("--TestBoundary--")
        );

        final Request request = builder.build();

        assertFalse(needsMoreData);
        assertTrue(parser.isFinished());
        assertEquals(Map.of(), request.getForm());
        assertEquals(Map.of(), request.getFiles());
    }

    /**
     * Tests parsing a multipart form containing one text field.
     *
     * @throws ServerException
     *     if the multipart data or request cannot be parsed.
     */
    @Test
    void parsesSingleTextField() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            Options.builder().build()
        );

        final boolean needsMoreData = parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"message\"\r\n"
                    + "\r\n"
                    + "Hello\r\n"
                    + "--TestBoundary--"
            )
        );

        final Request request = builder.build();

        assertFalse(needsMoreData);
        assertTrue(parser.isFinished());
        assertEquals(
            Map.of("message", List.of("Hello")),
            request.getForm()
        );
        assertEquals(Map.of(), request.getFiles());
    }

    /**
     * Tests parsing a multipart form containing several text fields.
     *
     * @throws ServerException
     *     if the multipart data or request cannot be parsed.
     */
    @Test
    void parsesSeveralTextFields() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            Options.builder().build()
        );

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"name\"\r\n"
                    + "\r\n"
                    + "Ivan\r\n"
                    + "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"age\"\r\n"
                    + "\r\n"
                    + "25\r\n"
                    + "--TestBoundary--"
            )
        );

        final Request request = builder.build();

        assertTrue(parser.isFinished());
        assertEquals(
            Map.of(
                "name", List.of("Ivan"),
                "age", List.of("25")
            ),
            request.getForm()
        );
    }

    /**
     * Tests parsing repeated form fields with the same name.
     *
     * @throws ServerException
     *     if the multipart data or request cannot be parsed.
     */
    @Test
    void parsesRepeatedTextFields() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            Options.builder().build()
        );

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"tag\"\r\n"
                    + "\r\n"
                    + "java\r\n"
                    + "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"tag\"\r\n"
                    + "\r\n"
                    + "http\r\n"
                    + "--TestBoundary--"
            )
        );

        final Request request = builder.build();

        assertEquals(
            List.of("java", "http"),
            request.getForm().get("tag")
        );
    }

    /**
     * Tests parsing a form field with an empty value.
     *
     * @throws ServerException
     *     if the multipart data or request cannot be parsed.
     */
    @Test
    void parsesEmptyTextField() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            Options.builder().build()
        );

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"empty\"\r\n"
                    + "\r\n"
                    + "\r\n"
                    + "--TestBoundary--"
            )
        );

        final Request request = builder.build();

        assertEquals(
            List.of(""),
            request.getForm().get("empty")
        );
    }

    /**
     * Tests parsing text containing non-ASCII characters.
     *
     * @throws ServerException
     *     if the multipart data or request cannot be parsed.
     */
    @Test
    void parsesUtf8TextField() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            Options.builder().build()
        );

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"message\"\r\n"
                    + "\r\n"
                    + "Привет, мир!\r\n"
                    + "--TestBoundary--"
            )
        );

        final Request request = builder.build();

        assertEquals(
            List.of("Привет, мир!"),
            request.getForm().get("message")
        );
    }

    /**
     * Tests accepting multipart data in several portions.
     *
     * @throws ServerException
     *     if the multipart data or request cannot be parsed.
     */
    @Test
    void acceptsMultipartDataInSeveralPortions() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            Options.builder().build()
        );

        assertTrue(parser.accept(createBytes("--Test")));
        assertTrue(parser.accept(createBytes("Boundary\r\nContent-")));
        assertTrue(parser.accept(createBytes(
            "Disposition: form-data; name=\"message\"\r\n\r\nHel"
        )));
        assertFalse(parser.accept(createBytes(
            "lo\r\n--TestBoundary--"
        )));

        final Request request = builder.build();

        assertTrue(parser.isFinished());
        assertEquals(
            List.of("Hello"),
            request.getForm().get("message")
        );
    }

    /**
     * Tests rejecting a {@code null} request builder.
     */
    @Test
    void rejectsNullBuilder() {
        final NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new MultipartParser(
                null,
                BOUNDARY,
                Options.builder().build()
            )
        );

        assertEquals(
            "Request builder must not be null.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting a {@code null} multipart boundary.
     */
    @Test
    void rejectsNullBoundary() {
        final NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new MultipartParser(
                Request.builder(),
                null,
                Options.builder().build()
            )
        );

        assertEquals(
            "Multipart boundary must not be null.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting an empty multipart boundary.
     */
    @Test
    void rejectsEmptyBoundary() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new MultipartParser(
                Request.builder(),
                "",
                Options.builder().build()
            )
        );

        assertEquals(
            "Multipart boundary must not be empty.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting {@code null} server options.
     */
    @Test
    void rejectsNullOptions() {
        final NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new MultipartParser(
                Request.builder(),
                BOUNDARY,
                null
            )
        );

        assertEquals(
            "Server options must not be null.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting a {@code null} data portion.
     */
    @Test
    void rejectsNullData() {
        final MultipartParser parser = new MultipartParser(
            Request.builder(),
            BOUNDARY,
            Options.builder().build()
        );

        final NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> parser.accept(null)
        );

        assertEquals(
            "Multipart data must not be null.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting additional data after the final multipart boundary.
     *
     * @throws ServerException
     *     if parsing the valid multipart body fails.
     */
    @Test
    void rejectsDataAfterCompletion() throws ServerException {
        final MultipartParser parser = new MultipartParser(
            Request.builder(),
            BOUNDARY,
            Options.builder().build()
        );

        parser.accept(createBytes("--TestBoundary--"));

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(createBytes("unexpected"))
        );

        assertEquals(
            "Multipart data has already been completely parsed.",
            exception.getMessage()
        );
    }

    /**
     * Creates a request builder containing a valid request header.
     *
     * @return
     *     the request builder.
     * @throws ServerException
     *     if the request header cannot be built.
     */
    private Request.Builder createRequestBuilder()
        throws ServerException {

        final RequestHeader header = RequestHeader.builder()
            .setMethod(HttpMethod.POST)
            .setTarget("/upload")
            .setVersion(HttpVersion.HTTP_1_1)
            .addValue(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
            .build();

        return Request.builder().setHeader(header);
    }

    /**
     * Converts the specified text to HTTP-compatible bytes.
     *
     * @param value
     *     the source text.
     * @return
     *     the encoded bytes.
     */
    private byte[] createBytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
