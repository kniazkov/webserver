/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.ServerException;
import com.kniazkov.webserver.UploadedFile;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests parsing uploaded files from multipart form data.
 */
final class MultipartParserFileTest extends MultipartParserBaseTest {

    /**
     * Tests parsing a single uploaded file.
     */
    @Test
    void singleFile() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"hello.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + "Hello, world!\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);

        assertTrue(request.getForm().isEmpty());

        final UploadedFile file =
            request.getFiles().get("file").get(0);

        assertEquals("hello.txt", file.getName());
        assertEquals(ContentType.TEXT_PLAIN, file.getContentType());
        assertArrayEquals(
            bytes("Hello, world!"),
            file.getData()
        );
    }

    /**
     * Tests parsing an empty uploaded file.
     */
    @Test
    void emptyFile() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"empty.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + "\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);
        final UploadedFile file =
            request.getFiles().get("file").get(0);

        assertEquals("empty.txt", file.getName());
        assertEquals(ContentType.TEXT_PLAIN, file.getContentType());
        assertArrayEquals(new byte[0], file.getData());
    }

    /**
     * Tests parsing a file without a Content-Type header.
     */
    @Test
    void fileWithoutContentType() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"data.bin\"\r\n"
                + "\r\n"
                + "some data\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);
        final UploadedFile file =
            request.getFiles().get("file").get(0);

        assertEquals("data.bin", file.getName());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            file.getContentType()
        );
        assertArrayEquals(bytes("some data"), file.getData());
    }

    /**
     * Tests parsing several files belonging to the same form field.
     */
    @Test
    void severalFilesForSameField() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"files\"; filename=\"first.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + "first\r\n"
                + "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"files\"; filename=\"second.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + "second\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);
        final List<UploadedFile> files =
            request.getFiles().get("files");

        assertEquals(2, files.size());

        assertEquals("first.txt", files.get(0).getName());
        assertArrayEquals(
            bytes("first"),
            files.get(0).getData()
        );

        assertEquals("second.txt", files.get(1).getName());
        assertArrayEquals(
            bytes("second"),
            files.get(1).getData()
        );
    }

    /**
     * Tests parsing files belonging to different form fields.
     */
    @Test
    void filesForDifferentFields() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"avatar\"; filename=\"avatar.png\"\r\n"
                + "Content-Type: image/png\r\n"
                + "\r\n"
                + "png-data\r\n"
                + "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"document\"; filename=\"document.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n"
                + "\r\n"
                + "pdf-data\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);

        assertEquals(
            Map.of(
                "avatar",
                List.of(request.getFiles().get("avatar").get(0)),
                "document",
                List.of(request.getFiles().get("document").get(0))
            ).keySet(),
            request.getFiles().keySet()
        );

        final UploadedFile avatar =
            request.getFiles().get("avatar").get(0);

        assertEquals("avatar.png", avatar.getName());
        assertEquals(ContentType.IMAGE_PNG, avatar.getContentType());
        assertArrayEquals(bytes("png-data"), avatar.getData());

        final UploadedFile document =
            request.getFiles().get("document").get(0);

        assertEquals("document.pdf", document.getName());
        assertEquals(
            ContentType.APPLICATION_PDF,
            document.getContentType()
        );
        assertArrayEquals(bytes("pdf-data"), document.getData());
    }

    /**
     * Tests parsing form fields and uploaded files together.
     */
    @Test
    void formAndFile() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"\r\n"
                + "\r\n"
                + "Document\r\n"
                + "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"document.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + "contents\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);

        assertEquals(
            Map.of(
                "title", List.of("Document")
            ),
            request.getForm()
        );

        final UploadedFile file =
            request.getFiles().get("file").get(0);

        assertEquals("document.txt", file.getName());
        assertEquals(ContentType.TEXT_PLAIN, file.getContentType());
        assertArrayEquals(bytes("contents"), file.getData());
    }

    /**
     * Tests recognition of a content type containing parameters.
     */
    @Test
    void contentTypeWithParameters() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"hello.txt\"\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "\r\n"
                + "Hello\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);
        final UploadedFile file =
            request.getFiles().get("file").get(0);

        assertEquals(ContentType.TEXT_PLAIN, file.getContentType());
    }

    /**
     * Tests a file whose size is exactly the configured limit.
     */
    @Test
    void exactFileSizeLimit() throws ServerException {
        final Options options = new Options.Builder()
            .setMaxFileSize(5)
            .build();

        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"data.bin\"\r\n"
                + "\r\n"
                + "12345\r\n"
                + "--" + BOUNDARY + "--",
            options
        );

        assertArrayEquals(
            bytes("12345"),
            request.getFiles().get("file").get(0).getData()
        );
    }

    /**
     * Tests a file exceeding the configured limit by one byte.
     */
    @Test
    void fileSizeLimitExceededByOne() {
        final Options options = new Options.Builder()
            .setMaxFileSize(5)
            .build();

        assertThrows(
            ServerException.class,
            () -> parse(
                "--" + BOUNDARY + "\r\n"
                    + "Content-Disposition: form-data; "
                    + "name=\"file\"; filename=\"data.bin\"\r\n"
                    + "\r\n"
                    + "123456\r\n"
                    + "--" + BOUNDARY + "--",
                options
            )
        );
    }

    /**
     * Tests that the file size limit is applied independently to each file.
     */
    @Test
    void fileLimitIsPerFile() throws ServerException {
        final Options options = new Options.Builder()
            .setMaxFileSize(5)
            .build();

        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"files\"; filename=\"one.bin\"\r\n"
                + "\r\n"
                + "12345\r\n"
                + "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"files\"; filename=\"two.bin\"\r\n"
                + "\r\n"
                + "abcde\r\n"
                + "--" + BOUNDARY + "--",
            options
        );

        assertEquals(2, request.getFiles().get("files").size());
    }

    /**
     * Tests that ordinary form fields are not limited by maxFileSize.
     */
    @Test
    void formFieldIsNotLimitedByFileSize() throws ServerException {
        final Options options = new Options.Builder()
            .setMaxFileSize(1)
            .build();

        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"field\"\r\n"
                + "\r\n"
                + "this is much larger than one byte\r\n"
                + "--" + BOUNDARY + "--",
            options
        );

        assertEquals(
            List.of("this is much larger than one byte"),
            request.getForm().get("field")
        );
        assertTrue(request.getFiles().isEmpty());
    }
}
