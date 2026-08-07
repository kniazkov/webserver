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
 * Tests multipart parsing using pseudo-random input chunk sizes.
 */
final class MultipartRandomStreamingTest {

    /**
     * The multipart boundary used by the tests.
     */
    private static final String BOUNDARY = "RandomBoundary";

    /**
     * The number of random parsing iterations.
     */
    private static final int ITERATIONS = 1000;

    /**
     * The maximum random chunk size.
     */
    private static final int MAX_CHUNK_SIZE = 64;

    /**
     * Tests parsing the same multipart request using many different random
     * chunk sequences.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesRandomChunkSequences() throws ServerException {
        final byte[] body = createMultipartBody();

        for (int seed = 0; seed < ITERATIONS; seed++) {
            final Request request = parseRandomly(body, seed);

            verifyRequest(request);
        }
    }

    /**
     * Tests parsing random binary file data using random input chunk sizes.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void preservesRandomBinaryData() throws ServerException {
        final Random random = new Random(123456789L);

        for (int iteration = 0; iteration < 100; iteration++) {
            final int size = 1 + random.nextInt(4096);
            final byte[] fileData = new byte[size];

            random.nextBytes(fileData);

            final byte[] body = createSingleFileBody(fileData);
            final Request request = parseRandomly(
                body,
                random.nextLong()
            );

            final UploadedFile file = request.getFiles()
                .get("file")
                .get(0);

            assertEquals(
                ContentType.APPLICATION_OCTET_STREAM,
                file.getContentType()
            );
            assertArrayEquals(fileData, file.getData());
        }
    }

    /**
     * Tests parsing with random chunk sequences that often split the input
     * into very small portions.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesMostlyTinyRandomChunks() throws ServerException {
        final byte[] body = createMultipartBody();

        for (int seed = 0; seed < 250; seed++) {
            final Request request = parseRandomly(
                body,
                seed,
                4
            );

            verifyRequest(request);
        }
    }

    /**
     * Tests that the parser requests more data until the final random chunk
     * completes the multipart body.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void reportsCompletionOnlyAfterFinalChunk() throws ServerException {
        final byte[] body = createMultipartBody();
        final List<byte[]> chunks = splitRandomly(
            body,
            new Random(42L),
            MAX_CHUNK_SIZE
        );

        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        for (int index = 0; index < chunks.size(); index++) {
            final boolean needsMoreData = parser.accept(
                chunks.get(index)
            );

            if (index < chunks.size() - 1) {
                assertTrue(needsMoreData);
                assertFalse(parser.isFinished());
            } else {
                assertFalse(needsMoreData);
                assertTrue(parser.isFinished());
            }
        }

        verifyRequest(builder.build());
    }

    /**
     * Tests that repeated parsing with the same random seed produces the same
     * request.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void producesDeterministicResultForSameSeed()
        throws ServerException {

        final byte[] body = createMultipartBody();

        final Request first = parseRandomly(body, 987654321L);
        final Request second = parseRandomly(body, 987654321L);

        assertEquals(first.getForm(), second.getForm());
        assertEquals(
            first.getFiles().keySet(),
            second.getFiles().keySet()
        );

        final UploadedFile firstFile = first.getFiles()
            .get("file")
            .get(0);
        final UploadedFile secondFile = second.getFiles()
            .get("file")
            .get(0);

        assertEquals(
            firstFile.getFileName(),
            secondFile.getFileName()
        );
        assertEquals(
            firstFile.getContentType(),
            secondFile.getContentType()
        );
        assertArrayEquals(
            firstFile.getData(),
            secondFile.getData()
        );
    }

    /**
     * Parses multipart data using pseudo-random chunk sizes.
     *
     * @param body
     *     the multipart request body.
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
        return parseRandomly(body, seed, MAX_CHUNK_SIZE);
    }

    /**
     * Parses multipart data using pseudo-random chunk sizes.
     *
     * @param body
     *     the multipart request body.
     * @param seed
     *     the random seed.
     * @param maximumChunkSize
     *     the maximum size of one chunk.
     * @return
     *     the parsed request.
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    private Request parseRandomly(
        final byte[] body,
        final long seed,
        final int maximumChunkSize
    ) throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        final List<byte[]> chunks = splitRandomly(
            body,
            new Random(seed),
            maximumChunkSize
        );

        for (byte[] chunk : chunks) {
            parser.accept(chunk);
        }

        assertTrue(parser.isFinished());

        return builder.build();
    }

    /**
     * Splits the specified byte array into pseudo-random chunks.
     *
     * @param source
     *     the source array.
     * @param random
     *     the random number generator.
     * @param maximumChunkSize
     *     the maximum chunk size.
     * @return
     *     the generated chunks.
     */
    private List<byte[]> splitRandomly(
        final byte[] source,
        final Random random,
        final int maximumChunkSize
    ) {
        final List<byte[]> result = new ArrayList<>();
        int offset = 0;

        while (offset < source.length) {
            final int remaining = source.length - offset;
            final int size = Math.min(
                remaining,
                1 + random.nextInt(maximumChunkSize)
            );

            result.add(
                copyOfRange(
                    source,
                    offset,
                    offset + size
                )
            );

            offset += size;
        }

        return result;
    }

    /**
     * Verifies the request produced from the standard multipart test body.
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

        assertEquals("random.bin", file.getFileName());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            file.getContentType()
        );
        assertArrayEquals(
            createBinaryData(),
            file.getData()
        );
    }

    /**
     * Creates the multipart body used by the random streaming tests.
     *
     * @return
     *     the multipart body.
     */
    private byte[] createMultipartBody() {
        final byte[] prefix = createBytes(
            "--RandomBoundary\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--RandomBoundary\r\n"
                + "Content-Disposition: form-data; name=\"tag\"\r\n"
                + "\r\n"
                + "java\r\n"
                + "--RandomBoundary\r\n"
                + "Content-Disposition: form-data; name=\"tag\"\r\n"
                + "\r\n"
                + "http\r\n"
                + "--RandomBoundary\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"random.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "\r\n"
        );

        final byte[] suffix = createBytes(
            "\r\n--RandomBoundary--"
        );

        return concatenate(
            prefix,
            createBinaryData(),
            suffix
        );
    }

    /**
     * Creates a multipart body containing one binary file.
     *
     * @param fileData
     *     the binary file data.
     * @return
     *     the multipart body.
     */
    private byte[] createSingleFileBody(final byte[] fileData) {
        final byte[] prefix = createBytes(
            "--RandomBoundary\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"random.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "\r\n"
        );

        final byte[] suffix = createBytes(
            "\r\n--RandomBoundary--"
        );

        return concatenate(prefix, fileData, suffix);
    }

    /**
     * Creates deterministic binary data used by the standard test request.
     *
     * @return
     *     the binary file data.
     */
    private byte[] createBinaryData() {
        final byte[] data = new byte[1024];

        for (int index = 0; index < data.length; index++) {
            data[index] = (byte) (
                index * 31 + index / 7
            );
        }

        return data;
    }

    /**
     * Creates a multipart parser using default server options.
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
     * Concatenates the specified byte arrays.
     *
     * @param arrays
     *     the arrays to concatenate.
     * @return
     *     the concatenated byte array.
     */
    private byte[] concatenate(final byte[]... arrays) {
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
