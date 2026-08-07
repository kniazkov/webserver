/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests multipart request and uploaded file size limits.
 */
final class MultipartLimitTest {

    /**
     * The multipart boundary used by the tests.
     */
    private static final String BOUNDARY = "TestBoundary";

    /**
     * Tests rejecting a multipart request that exceeds the maximum request
     * size.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsRequestExceedingMaximumSize() throws ServerException {
        final byte[] body = createMultipartBody(
            "file",
            "test.bin",
            new byte[] {1, 2, 3, 4}
        );

        final Options options = Options.builder()
            .setMaxRequestSize(body.length - 1L)
            .setMaxFileSize(body.length - 1L)
            .build();

        final MultipartParser parser = new MultipartParser(
            createRequestBuilder(),
            BOUNDARY,
            options
        );

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(body)
        );

        assertEquals(
            "Maximum request size has been exceeded.",
            exception.getMessage()
        );
    }

    /**
     * Tests accepting a multipart request whose size exactly matches the
     * maximum request size.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void acceptsRequestAtMaximumSize() throws ServerException {
        final byte[] fileData = new byte[] {1, 2, 3, 4};
        final byte[] body = createMultipartBody(
            "file",
            "test.bin",
            fileData
        );

        final Options options = Options.builder()
            .setMaxRequestSize(body.length)
            .setMaxFileSize(fileData.length)
            .build();

        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            options
        );

        final boolean needsMoreData = parser.accept(body);

        assertFalse(needsMoreData);
        assertTrue(parser.isFinished());
        assertArrayEquals(
            fileData,
            builder.build()
                .getFiles()
                .get("file")
                .get(0)
                .getData()
        );
    }

    /**
     * Tests rejecting an uploaded file that exceeds the maximum file size.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void rejectsFileExceedingMaximumSize() throws ServerException {
        final byte[] fileData = new byte[] {
            1, 2, 3, 4, 5
        };
        final byte[] body = createMultipartBody(
            "file",
            "test.bin",
            fileData
        );

        final Options options = Options.builder()
            .setMaxRequestSize(body.length)
            .setMaxFileSize(fileData.length - 1L)
            .build();

        final MultipartParser parser = new MultipartParser(
            createRequestBuilder(),
            BOUNDARY,
            options
        );

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(body)
        );

        assertEquals(
            "Maximum uploaded file size has been exceeded.",
            exception.getMessage()
        );
    }

    /**
     * Tests accepting an uploaded file whose size exactly matches the maximum
     * file size.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void acceptsFileAtMaximumSize() throws ServerException {
        final byte[] fileData = new byte[] {
            0, 1, 2, 3, 4, 5, 6, 7
        };
        final byte[] body = createMultipartBody(
            "file",
            "test.bin",
            fileData
        );

        final Options options = Options.builder()
            .setMaxRequestSize(body.length)
            .setMaxFileSize(fileData.length)
            .build();

        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            options
        );

        parser.accept(body);

        final UploadedFile file = builder.build()
            .getFiles()
            .get("file")
            .get(0);

        assertEquals(fileData.length, file.getSize());
        assertArrayEquals(fileData, file.getData());
    }

    /**
     * Tests enforcing the maximum file size when file data arrives in several
     * portions.
     *
     * @throws ServerException
     *     if request initialization or initial parsing fails.
     */
    @Test
    void rejectsOversizedFileReceivedInSeveralChunks()
        throws ServerException {

        final byte[] fileData = new byte[64];
        final byte[] body = createMultipartBody(
            "file",
            "test.bin",
            fileData
        );

        final Options options = Options.builder()
            .setMaxRequestSize(body.length)
            .setMaxFileSize(32)
            .build();

        final MultipartParser parser = new MultipartParser(
            createRequestBuilder(),
            BOUNDARY,
            options
        );

        int offset = 0;
        ServerException exception = null;

        while (offset < body.length && exception == null) {
            final int end = Math.min(offset + 7, body.length);
            final byte[] chunk = copyOfRange(body, offset, end);

            try {
                parser.accept(chunk);
            } catch (ServerException error) {
                exception = error;
            }

            offset = end;
        }

        assertEquals(
            "Maximum uploaded file size has been exceeded.",
            exception == null ? null : exception.getMessage()
        );
    }

    /**
     * Tests enforcing the maximum request size across several input portions.
     *
     * @throws ServerException
     *     if request initialization or initial parsing fails.
     */
    @Test
    void rejectsOversizedRequestReceivedInSeveralChunks()
        throws ServerException {

        final byte[] body = createMultipartBody(
            "file",
            "test.bin",
            new byte[32]
        );

        final long limit = body.length - 5L;

        final Options options = Options.builder()
            .setMaxRequestSize(limit)
            .setMaxFileSize(32)
            .build();

        final MultipartParser parser = new MultipartParser(
            createRequestBuilder(),
            BOUNDARY,
            options
        );

        int offset = 0;
        ServerException exception = null;

        while (offset < body.length && exception == null) {
            final int end = Math.min(offset + 11, body.length);
            final byte[] chunk = copyOfRange(body, offset, end);

            try {
                parser.accept(chunk);
            } catch (ServerException error) {
                exception = error;
            }

            offset = end;
        }

        assertEquals(
            "Maximum request size has been exceeded.",
            exception == null ? null : exception.getMessage()
        );
    }

    /**
     * Tests accepting several files when each individual file is within the
     * configured file size limit.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void acceptsSeveralFilesWithinIndividualLimit()
        throws ServerException {

        final byte[] firstData = new byte[32];
        final byte[] secondData = new byte[32];

        final byte[] body = createMultipartBody(
            createFilePart("files", "first.bin", firstData),
            createFilePart("files", "second.bin", secondData)
        );

        final Options options = Options.builder()
            .setMaxRequestSize(body.length)
            .setMaxFileSize(32)
            .build();

        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = new MultipartParser(
            builder,
            BOUNDARY,
            options
        );

        final boolean needsMoreData = parser.accept(body);

        final Request request = builder.build();

        assertFalse(needsMoreData);
        assertEquals(2, request.getFiles().get("files").size());
        assertEquals(
            32,
            request.getFiles().get("files").get(0).getSize()
        );
        assertEquals(
            32,
            request.getFiles().get("files").get(1).getSize()
        );
    }

    /**
     * Tests that text form data contributes to the total request size limit.
     *
     * @throws ServerException
     *     if request initialization fails.
     */
    @Test
    void countsTextFieldsTowardRequestLimit() throws ServerException {
        final byte[] body = (
            "--TestBoundary\r\n"
                + "Content-Disposition: form-data; name=\"message\"\r\n"
                + "\r\n"
                + "This text is part of the request body.\r\n"
                + "--TestBoundary--"
        ).getBytes(StandardCharsets.UTF_8);

        final Options options = Options.builder()
            .setMaxRequestSize(body.length - 1L)
            .setMaxFileSize(body.length - 1L)
            .build();

        final MultipartParser parser = new MultipartParser(
            createRequestBuilder(),
            BOUNDARY,
            options
        );

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parser.accept(body)
        );

        assertEquals(
            "Maximum request size has been exceeded.",
            exception.getMessage()
        );
    }

    /**
     * Creates a complete multipart body containing one uploaded file.
     *
     * @param fieldName
     *     the form field name.
     * @param fileName
     *     the uploaded file name.
     * @param data
     *     the file data.
     * @return
     *     the complete multipart body.
     */
    private byte[] createMultipartBody(
        final String fieldName,
        final String fileName,
        final byte[] data
    ) {
        return createMultipartBody(
            createFilePart(fieldName, fileName, data)
        );
    }

    /**
     * Creates a complete multipart body from the specified parts.
     *
     * @param parts
     *     the multipart parts without their leading boundaries.
     * @return
     *     the complete multipart body.
     */
    private byte[] createMultipartBody(final byte[]... parts) {
        int length = 0;

        final byte[] firstBoundary = (
            "--" + BOUNDARY + "\r\n"
        ).getBytes(StandardCharsets.UTF_8);

        final byte[] nextBoundary = (
            "\r\n--" + BOUNDARY + "\r\n"
        ).getBytes(StandardCharsets.UTF_8);

        final byte[] finalBoundary = (
            "\r\n--" + BOUNDARY + "--"
        ).getBytes(StandardCharsets.UTF_8);

        length += firstBoundary.length;
        length += finalBoundary.length;

        for (int index = 0; index < parts.length; index++) {
            length += parts[index].length;

            if (index > 0) {
                length += nextBoundary.length;
            }
        }

        final byte[] result = new byte[length];
        int offset = 0;

        offset = copy(firstBoundary, result, offset);

        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                offset = copy(nextBoundary, result, offset);
            }

            offset = copy(parts[index], result, offset);
        }

        copy(finalBoundary, result, offset);

        return result;
    }

    /**
     * Creates a multipart file part without its leading boundary.
     *
     * @param fieldName
     *     the form field name.
     * @param fileName
     *     the uploaded file name.
     * @param data
     *     the file data.
     * @return
     *     the multipart part.
     */
    private byte[] createFilePart(
        final String fieldName,
        final String fileName,
        final byte[] data
    ) {
        final byte[] header = (
            "Content-Disposition: form-data; name=\""
                + fieldName
                + "\"; filename=\""
                + fileName
                + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "\r\n"
        ).getBytes(StandardCharsets.UTF_8);

        final byte[] result = new byte[header.length + data.length];

        System.arraycopy(
            header,
            0,
            result,
            0,
            header.length
        );
        System.arraycopy(
            data,
            0,
            result,
            header.length,
            data.length
        );

        return result;
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
     * Copies one byte array into another.
     *
     * @param source
     *     the source array.
     * @param target
     *     the target array.
     * @param offset
     *     the target offset.
     * @return
     *     the offset immediately after the copied data.
     */
    private int copy(
        final byte[] source,
        final byte[] target,
        final int offset
    ) {
        System.arraycopy(
            source,
            0,
            target,
            offset,
            source.length
        );

        return offset + source.length;
    }

    /**
     * Creates a copy of the specified byte array range.
     *
     * @param source
     *     the source array.
     * @param from
     *     the first included index.
     * @param to
     *     the first excluded index.
     * @return
     *     the copied range.
     */
    private byte[] copyOfRange(
        final byte[] source,
        final int from,
        final int to
    ) {
        final byte[] result = new byte[to - from];

        System.arraycopy(
            source,
            from,
            result,
            0,
            result.length
        );

        return result;
    }
}
