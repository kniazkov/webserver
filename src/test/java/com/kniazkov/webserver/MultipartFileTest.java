package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests multipart file parsing performed by the {@link MultipartParser}.
 */
final class MultipartFileTest {

    /**
     * The multipart boundary used by the tests.
     */
    private static final String BOUNDARY = "TestBoundary";

    /**
     * Tests parsing a single uploaded file.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesSingleFile() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        final boolean needsMoreData = parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"file\"; filename=\"hello.txt\"\r\n"
                    + "Content-Type: text/plain\r\n"
                    + "\r\n"
                    + "Hello\r\n"
                    + "--TestBoundary--"
            )
        );

        final Request request = builder.build();
        final UploadedFile file = request.getFiles()
            .get("file")
            .get(0);

        assertFalse(needsMoreData);
        assertTrue(parser.isFinished());
        assertEquals("hello.txt", file.getFileName());
        assertEquals(ContentType.TEXT_PLAIN, file.getContentType());
        assertArrayEquals(
            "Hello".getBytes(StandardCharsets.UTF_8),
            file.getData()
        );
    }

    /**
     * Tests parsing several uploaded files from different form fields.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesSeveralFiles() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"avatar\"; filename=\"avatar.png\"\r\n"
                    + "Content-Type: image/png\r\n"
                    + "\r\n"
                    + "PNG\r\n"
                    + "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"document\"; filename=\"document.pdf\"\r\n"
                    + "Content-Type: application/pdf\r\n"
                    + "\r\n"
                    + "PDF\r\n"
                    + "--TestBoundary--"
            )
        );

        final Request request = builder.build();

        final UploadedFile avatar = request.getFiles()
            .get("avatar")
            .get(0);
        final UploadedFile document = request.getFiles()
            .get("document")
            .get(0);

        assertEquals("avatar.png", avatar.getFileName());
        assertEquals(ContentType.IMAGE_PNG, avatar.getContentType());
        assertArrayEquals(
            createBytes("PNG"),
            avatar.getData()
        );

        assertEquals("document.pdf", document.getFileName());
        assertEquals(ContentType.APPLICATION_PDF, document.getContentType());
        assertArrayEquals(
            createBytes("PDF"),
            document.getData()
        );
    }

    /**
     * Tests parsing several uploaded files using the same form field name.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesSeveralFilesWithSameFieldName() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"images\"; filename=\"first.jpg\"\r\n"
                    + "Content-Type: image/jpeg\r\n"
                    + "\r\n"
                    + "first\r\n"
                    + "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"images\"; filename=\"second.jpg\"\r\n"
                    + "Content-Type: image/jpeg\r\n"
                    + "\r\n"
                    + "second\r\n"
                    + "--TestBoundary--"
            )
        );

        final Request request = builder.build();
        final List<UploadedFile> files = request.getFiles().get("images");

        assertEquals(2, files.size());

        assertEquals("first.jpg", files.get(0).getFileName());
        assertArrayEquals(
            createBytes("first"),
            files.get(0).getData()
        );

        assertEquals("second.jpg", files.get(1).getFileName());
        assertArrayEquals(
            createBytes("second"),
            files.get(1).getData()
        );
    }

    /**
     * Tests parsing an empty uploaded file.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesEmptyFile() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"file\"; filename=\"empty.txt\"\r\n"
                    + "Content-Type: text/plain\r\n"
                    + "\r\n"
                    + "\r\n"
                    + "--TestBoundary--"
            )
        );

        final UploadedFile file = builder.build()
            .getFiles()
            .get("file")
            .get(0);

        assertEquals("empty.txt", file.getFileName());
        assertEquals(ContentType.TEXT_PLAIN, file.getContentType());
        assertArrayEquals(new byte[0], file.getData());
        assertEquals(0, file.getSize());
    }

    /**
     * Tests parsing binary file data without text conversion.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void preservesBinaryFileData() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        final byte[] prefix = createBytes(
            "--TestBoundary\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"binary.dat\"\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "\r\n"
        );

        final byte[] fileData = new byte[] {
            0,
            1,
            2,
            3,
            10,
            13,
            (byte) 0x80,
            (byte) 0xFF
        };

        final byte[] suffix = createBytes(
            "\r\n--TestBoundary--"
        );

        parser.accept(concatenate(prefix, fileData, suffix));

        final UploadedFile file = builder.build()
            .getFiles()
            .get("file")
            .get(0);

        assertEquals("binary.dat", file.getFileName());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            file.getContentType()
        );
        assertArrayEquals(fileData, file.getData());
    }

    /**
     * Tests parsing a file without an explicit Content-Type header.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void usesBinaryContentTypeWhenMissing() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"file\"; filename=\"unknown.bin\"\r\n"
                    + "\r\n"
                    + "data\r\n"
                    + "--TestBoundary--"
            )
        );

        final UploadedFile file = builder.build()
            .getFiles()
            .get("file")
            .get(0);

        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            file.getContentType()
        );
    }

    /**
     * Tests parsing a text field and an uploaded file in the same request.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesFileAndTextField() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; name=\"title\"\r\n"
                    + "\r\n"
                    + "My document\r\n"
                    + "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"file\"; filename=\"document.txt\"\r\n"
                    + "Content-Type: text/plain\r\n"
                    + "\r\n"
                    + "contents\r\n"
                    + "--TestBoundary--"
            )
        );

        final Request request = builder.build();

        assertEquals(
            List.of("My document"),
            request.getForm().get("title")
        );

        final UploadedFile file = request.getFiles()
            .get("file")
            .get(0);

        assertEquals("document.txt", file.getFileName());
        assertArrayEquals(
            createBytes("contents"),
            file.getData()
        );
    }

    /**
     * Tests parsing a HEIC image upload.
     *
     * @throws ServerException
     *     if multipart data or request data cannot be parsed.
     */
    @Test
    void parsesHeicFile() throws ServerException {
        final Request.Builder builder = createRequestBuilder();
        final MultipartParser parser = createParser(builder);

        parser.accept(
            createBytes(
                "--TestBoundary\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"photo\"; filename=\"image.heic\"\r\n"
                    + "Content-Type: image/heic\r\n"
                    + "\r\n"
                    + "heic-data\r\n"
                    + "--TestBoundary--"
            )
        );

        final UploadedFile file = builder.build()
            .getFiles()
            .get("photo")
            .get(0);

        assertEquals("image.heic", file.getFileName());
        assertEquals(ContentType.IMAGE_HEIC, file.getContentType());
        assertArrayEquals(
            createBytes("heic-data"),
            file.getData()
        );
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
     * Concatenates several byte arrays.
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
}
