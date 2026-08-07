/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests incremental parsing performed by the {@link MultipartParser}.
 */
final class MultipartStreamingTest {

    /**
     * The multipart boundary used by the tests.
     */
    private static final String BOUNDARY = "TestBoundary";

    /**
     * Tests parsing multipart data one byte at a time.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesOneByteAtATime() throws ServerException {
        final byte[] body = createMultipartBody();
        final Request request = parseInChunks(body, 1);

        verifyRequest(request);
    }

    /**
     * Tests parsing multipart data two bytes at a time.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesTwoBytesAtATime() throws ServerException {
        final byte[] body = createMultipartBody();
        final Request request = parseInChunks(body, 2);

        verifyRequest(request);
    }

    /**
     * Tests parsing multipart data using small fixed-size chunks.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesSmallChunks() throws ServerException {
        final byte[] body = createMultipartBody();

        for (int size = 1; size <= 32; size++) {
            final Request request = parseInChunks(body, size);
            verifyRequest(request);
        }
    }

    /**
     * Tests parsing when the first boundary is split between input portions.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesSplitFirstBoundary() throws ServerException {
        final byte[] body = createMultipartBody();

        final Request request = parseAtOffsets(
            body,
            2,
            5,
            9,
            13
        );

        verifyRequest(request);
    }

    /**
     * Tests parsing when the part header separator is split between portions.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesSplitHeaderSeparator() throws ServerException {
        final byte[] body = createMultipartBody();

        final int separator = indexOf(
            body,
            "\r\n\r\n".getBytes(StandardCharsets.UTF_8)
        );

        final Request request = parseAtOffsets(
            body,
            separator + 1,
            separator + 2,
            separator + 3
        );

        verifyRequest(request);
    }

    /**
     * Tests parsing when a multipart boundary is split between portions.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesSplitPartBoundary() throws ServerException {
        final byte[] body = createMultipartBody();
        final byte[] marker = (
            "\r\n--" + BOUNDARY
        ).getBytes(StandardCharsets.UTF_8);

        final int boundary = indexOf(body, marker);

        final Request request = parseAtOffsets(
            body,
            boundary + 3,
            boundary + 6,
            boundary + marker.length - 1
        );

        verifyRequest(request);
    }

    /**
     * Tests parsing when the final boundary is split between portions.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesSplitFinalBoundary() throws ServerException {
        final byte[] body = createMultipartBody();
        final byte[] marker = (
            "\r\n--" + BOUNDARY + "--"
        ).getBytes(StandardCharsets.UTF_8);

        final int boundary = lastIndexOf(body, marker);

        final Request request = parseAtOffsets(
            body,
            boundary + 2,
            boundary + 7,
            boundary + marker.length - 2,
            boundary + marker.length - 1
        );

        verifyRequest(request);
    }

    /**
     * Tests parsing when file data is split between many portions.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesSplitFileData() throws ServerException {
        final byte[] body = createMultipartBody();
        final byte[] fileData = createFileData();

        final int start = indexOf(body, fileData);

        final Request request = parseAtOffsets(
            body,
            start + 1,
            start + 3,
            start + 7,
            start + 15,
            start + 31,
            start + fileData.length - 1
        );

        verifyRequest(request);
    }

    /**
     * Tests parsing using deterministic pseudo-random chunk sizes.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesRandomChunks() throws ServerException {
        final byte[] body = createMultipartBody();

        for (int seed = 0; seed < 100; seed++) {
            final Request request = parseRandomly(body, seed);
            verifyRequest(request);
        }
    }

    /**
     * Tests that the parser reports that more data is required until the final
     * multipart boundary is received.
     *
     * @throws ServerException
     *     if multipart data cannot be parsed.
     */
    @Test
    void reportsNeedForMoreData() throws ServerException {
        final byte[] body = createMultipartBody();
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        for (int index = 0; index < body.length - 1; index++) {
            assertTrue(
                parser.accept(new byte[] {body[index]})
            );
        }

        assertFalse(
            parser.accept(
                new byte[] {body[body.length - 1]}
            )
        );
        assertTrue(parser.isFinished());

        verifyRequest(builder.build());
    }

    /**
     * Tests that partial multipart parts are not added to the request builder.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void doesNotExposeIncompletePart() throws ServerException {
        final byte[] body = createMultipartBody();
        final byte[] firstBoundary = (
            "\r\n--" + BOUNDARY
        ).getBytes(StandardCharsets.UTF_8);

        final int firstPartEnd = indexOf(body, firstBoundary);

        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        parser.accept(copyOfRange(body, 0, firstPartEnd - 1));

        final Request incomplete = builder.build();

        assertTrue(incomplete.getForm().isEmpty());
        assertTrue(incomplete.getFiles().isEmpty());

        parser.accept(copyOfRange(body, firstPartEnd - 1, body.length));

        verifyRequest(builder.build());
    }

    /**
     * Parses multipart data using a fixed chunk size.
     *
     * @param body
     *     the multipart body.
     * @param chunkSize
     *     the maximum chunk size.
     * @return
     *     the parsed request.
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    private Request parseInChunks(
        final byte[] body,
        final int chunkSize
    ) throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        int offset = 0;

        while (offset < body.length) {
            final int end = Math.min(
                offset + chunkSize,
                body.length
            );

            parser.accept(copyOfRange(body, offset, end));
            offset = end;
        }

        assertTrue(parser.isFinished());

        return builder.build();
    }

    /**
     * Parses multipart data split at the specified absolute offsets.
     *
     * @param body
     *     the multipart body.
     * @param offsets
     *     the offsets at which the input is split.
     * @return
     *     the parsed request.
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    private Request parseAtOffsets(
        final byte[] body,
        final int... offsets
    ) throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        int start = 0;

        for (int end : offsets) {
            parser.accept(copyOfRange(body, start, end));
            start = end;
        }

        parser.accept(copyOfRange(body, start, body.length));

        assertTrue(parser.isFinished());

        return builder.build();
    }

    /**
     * Parses multipart data using deterministic random chunk sizes.
     *
     * @param body
     *     the multipart body.
     * @param seed
     *     the random seed.
     * @return
     *     the parsed request.
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    private Request parseRandomly(
        final byte[] body,
        final long seed
    ) throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);
        final Random random = new Random(seed);

        int offset = 0;

        while (offset < body.length) {
            final int remaining = body.length - offset;
            final int size = Math.min(
                remaining,
                1 + random.nextInt(32)
            );

            parser.accept(
                copyOfRange(body, offset, offset + size)
            );
            offset += size;
        }

        assertTrue(parser.isFinished());

        return builder.build();
    }

    /**
     * Verifies the request produced from the test multipart body.
     *
     * @param request
     *     the parsed request.
     */
    private void verifyRequest(final Request request) {
        assertEquals(
            List.of("Ivan"),
            request.getForm().get("name")
        );

        assertEquals(
            List.of("java", "http"),
            request.getForm().get("tag")
        );

        final List<UploadedFile> files =
            request.getFiles().get("file");

        assertEquals(1, files.size());

        final UploadedFile file = files.get(0);

        assertEquals("binary.dat", file.getFileName());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            file.getContentType()
        );
        assertArrayEquals(
            createFileData(),
            file.getData()
        );
    }

    /**
     * Creates the multipart body used by streaming tests.
     *
     * @return
     *     the multipart body.
     */
    private byte[] createMultipartBody() {
        final List<byte[]> parts = new ArrayList<>();

        parts.add(createBytes(
            "--TestBoundary\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--TestBoundary\r\n"
                + "Content-Disposition: form-data; name=\"tag\"\r\n"
                + "\r\n"
                + "java\r\n"
                + "--TestBoundary\r\n"
                + "Content-Disposition: form-data; name=\"tag\"\r\n"
                + "\r\n"
                + "http\r\n"
                + "--TestBoundary\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"binary.dat\"\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "\r\n"
        ));

        parts.add(createFileData());

        parts.add(createBytes(
            "\r\n--TestBoundary--"
        ));

        return concatenate(parts);
    }

    /**
     * Creates binary file data containing arbitrary byte values.
     *
     * @return
     *     the binary file data.
     */
    private byte[] createFileData() {
        final byte[] data = new byte[256];

        for (int index = 0; index < data.length; index++) {
            data[index] = (byte) index;
        }

        return data;
    }

    /**
     * Creates a multipart parser using default server options.
     *
     * @param builder
     *     the request builder.
     * @return
     *     the parser.
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
     * Converts text to UTF-8 bytes.
     *
     * @param value
     *     the source text.
     * @return
     *     the encoded bytes.
     */
    private byte[] createBytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
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

    /**
     * Concatenates a list of byte arrays.
     *
     * @param arrays
     *     the arrays to concatenate.
     * @return
     *     the concatenated array.
     */
    private byte[] concatenate(final List<byte[]> arrays) {
        int length = 0;

        for (byte[] array : arrays) {
            length += array.length;
        }

        final byte[] result = new byte[length];
        int offset = 0;

        for (byte[] array : arrays) {
            System.arraycopy(
                array,
                0,
                result,
                offset,
                array.length
            );
            offset += array.length;
        }

        return result;
    }

    /**
     * Finds the first occurrence of a byte sequence.
     *
     * @param source
     *     the source array.
     * @param target
     *     the sequence to find.
     * @return
     *     the sequence position, or {@code -1}.
     */
    private int indexOf(
        final byte[] source,
        final byte[] target
    ) {
        for (
            int index = 0;
            index <= source.length - target.length;
            index++
        ) {
            boolean matches = true;

            for (int offset = 0; offset < target.length; offset++) {
                if (source[index + offset] != target[offset]) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Finds the last occurrence of a byte sequence.
     *
     * @param source
     *     the source array.
     * @param target
     *     the sequence to find.
     * @return
     *     the sequence position, or {@code -1}.
     */
    private int lastIndexOf(
        final byte[] source,
        final byte[] target
    ) {
        for (
            int index = source.length - target.length;
            index >= 0;
            index--
        ) {
            boolean matches = true;

            for (int offset = 0; offset < target.length; offset++) {
                if (source[index + offset] != target[offset]) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return index;
            }
        }

        return -1;
    }
}
