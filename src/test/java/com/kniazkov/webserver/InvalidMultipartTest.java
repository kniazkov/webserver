/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests rejection of malformed multipart data by the
 * {@link MultipartParser}.
 */
final class InvalidMultipartTest {

    /**
     * The multipart boundary used by the tests.
     */
    private static final String BOUNDARY = "TestBoundary";

    /**
     * Tests rejecting a multipart body that starts with an unexpected
     * boundary.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsWrongFirstBoundary() throws ServerException {
        final MultipartParser parser = createParser();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(
                createBytes(
                    "--WrongBoundary\r\n"
                        + "Content-Disposition: form-data; name=\"value\"\r\n"
                        + "\r\n"
                        + "test\r\n"
                        + "--WrongBoundary--"
                )
            )
        );

        assertEquals(
            "Multipart body does not start with the expected boundary.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting an invalid first boundary received incrementally.
     *
     * @throws ServerException
     *     if request initialization or initial parsing fails.
     */
    @Test
    void rejectsInvalidPartialFirstBoundary() throws ServerException {
        final MultipartParser parser = createParser();

        assertTrue(parser.accept(createBytes("--Test")));

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(createBytes("Wrong"))
        );

        assertEquals(
            "Multipart body starts with an invalid boundary.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting a malformed multipart part header.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsMalformedPartHeader() throws ServerException {
        final MultipartParser parser = createParser();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(
                createBytes(
                    "--TestBoundary\r\n"
                        + "Content-Disposition form-data; name=\"value\"\r\n"
                        + "\r\n"
                        + "test\r\n"
                        + "--TestBoundary--"
                )
            )
        );

        assertEquals(
            "Malformed multipart part header: "
                + "Content-Disposition form-data; name=\"value\"",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting a multipart part without a Content-Disposition header.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsMissingContentDisposition() throws ServerException {
        final MultipartParser parser = createParser();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(
                createBytes(
                    "--TestBoundary\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "\r\n"
                        + "test\r\n"
                        + "--TestBoundary--"
                )
            )
        );

        assertEquals(
            "Multipart part does not contain Content-Disposition.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting a multipart part without a form field name.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsMissingFieldName() throws ServerException {
        final MultipartParser parser = createParser();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(
                createBytes(
                    "--TestBoundary\r\n"
                        + "Content-Disposition: form-data\r\n"
                        + "\r\n"
                        + "test\r\n"
                        + "--TestBoundary--"
                )
            )
        );

        assertEquals(
            "Multipart part does not contain a field name.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting an empty form field name.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsEmptyFieldName() throws ServerException {
        final MultipartParser parser = createParser();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(
                createBytes(
                    "--TestBoundary\r\n"
                        + "Content-Disposition: form-data; name=\"\"\r\n"
                        + "\r\n"
                        + "test\r\n"
                        + "--TestBoundary--"
                )
            )
        );

        assertEquals(
            "Multipart part does not contain a field name.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting an invalid boundary suffix after the first boundary.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsInvalidFirstBoundarySuffix() throws ServerException {
        final MultipartParser parser = createParser();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(
                createBytes(
                    "--TestBoundaryXX"
                )
            )
        );

        assertEquals(
            "Malformed multipart boundary suffix.",
            exception.getMessage()
        );
    }

    /**
     * Tests rejecting an invalid boundary suffix between multipart parts.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsInvalidPartBoundarySuffix() throws ServerException {
        final MultipartParser parser = createParser();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(
                createBytes(
                    "--TestBoundary\r\n"
                        + "Content-Disposition: form-data; name=\"first\"\r\n"
                        + "\r\n"
                        + "value\r\n"
                        + "--TestBoundaryXX"
                )
            )
        );

        assertEquals(
            "Malformed multipart boundary suffix.",
            exception.getMessage()
        );
    }

    /**
     * Tests that an incomplete header section requires additional data rather
     * than producing an invalid request.
     *
     * @throws ServerException
     *     if multipart data cannot be parsed.
     */
    @Test
    void waitsForIncompleteHeaders() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        final boolean needsMoreData = parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"value\"\r\n"
            )
        );

        assertTrue(needsMoreData);
        assertFalse(parser.isFinished());
        assertTrue(builder.build().getForm().isEmpty());
    }

    /**
     * Tests that incomplete part data is not exposed through the request
     * builder.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void doesNotExposeIncompletePart() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        final boolean needsMoreData = parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"value\"\r\n"
                    + "\r\n"
                    + "unfinished"
            )
        );

        assertTrue(needsMoreData);
        assertFalse(parser.isFinished());
        assertTrue(builder.build().getForm().isEmpty());
    }

    /**
     * Tests that an incomplete final boundary keeps the parser unfinished.
     *
     * @throws ServerException
     *     if multipart data cannot be parsed.
     */
    @Test
    void waitsForIncompleteFinalBoundary() throws ServerException {
        final MultipartParser parser = createParser();

        final boolean needsMoreData = parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"value\"\r\n"
                    + "\r\n"
                    + "test\r\n"
                    + "--TestBoundary-"
            )
        );

        assertTrue(needsMoreData);
        assertFalse(parser.isFinished());
    }

    /**
     * Tests completing a final boundary that was split between calls.
     *
     * @throws ServerException
     *     if multipart data cannot be parsed.
     */
    @Test
    void completesSplitFinalBoundary() throws ServerException {
        final MultipartParser parser = createParser();

        assertTrue(
            parser.accept(
                createBytes(
                    "--TestBoundary\r\n"
                        + "Content-Disposition: form-data; name=\"value\"\r\n"
                        + "\r\n"
                        + "test\r\n"
                        + "--TestBoundary-"
                )
            )
        );

        assertFalse(parser.accept(createBytes("-")));
        assertTrue(parser.isFinished());
    }

    /**
     * Tests rejecting additional input after the final multipart boundary.
     *
     * @throws ServerException
     *     if parsing the valid multipart body fails.
     */
    @Test
    void rejectsInputAfterCompletion() throws ServerException {
        final MultipartParser parser = createParser();

        assertFalse(
            parser.accept(
                createBytes("--TestBoundary--")
            )
        );

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
     * Tests rejecting a null input array.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsNullInput() throws ServerException {
        final MultipartParser parser = createParser();

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
     * Creates a multipart parser using default server options.
     *
     * @return
     *     the multipart parser.
     * @throws ServerException
     *     if the request header cannot be built.
     */
    private MultipartParser createParser() throws ServerException {
        return createParser(createRequestBuilder());
    }

    /**
     * Creates a multipart parser using the specified request builder.
     *
     * @param builder
     *     the request builder populated by the parser.
     * @return
     *     the multipart parser.
     */
    private MultipartParser createParser(
        final Request.Builder builder
    ) {
        return new MultipartParser(
            builder,
            BOUNDARY,
            Options.builder().build()
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
            .addValue(
                HttpHeaders.CONTENT_TYPE,
                "multipart/form-data; boundary=" + BOUNDARY
            )
            .build();

        return Request.builder().setHeader(header);
    }

    /**
     * Converts the specified text to UTF-8 bytes.
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
