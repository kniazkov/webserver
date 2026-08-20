/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.ServerException;

import com.kniazkov.webserver.UploadedFile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing valid simple multipart form data.
 */
final class MultipartParserTest extends MultipartParserBaseTest {

    /**
     * Tests a multipart request containing one form field.
     */
    @Test
    void simpleForm() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);

        assertEquals(
            Map.of(
                "name", List.of("Ivan")
            ),
            request.getForm()
        );
        assertTrue(request.getFiles().isEmpty());
    }

    /**
     * Tests a multipart request containing several form fields.
     */
    @Test
    void severalFields() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"city\"\r\n"
                + "\r\n"
                + "Prague\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);

        assertEquals(
            Map.of(
                "name", List.of("Ivan"),
                "city", List.of("Prague")
            ),
            request.getForm()
        );
    }

    /**
     * Tests repeated form fields.
     */
    @Test
    void repeatedField() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"value\"\r\n"
                + "\r\n"
                + "first\r\n"
                + "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"value\"\r\n"
                + "\r\n"
                + "second\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);

        assertEquals(
            Map.of(
                "value", List.of("first", "second")
            ),
            request.getForm()
        );
    }

    /**
     * Tests an empty form field.
     */
    @Test
    void emptyField() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"empty\"\r\n"
                + "\r\n"
                + "\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);

        assertEquals(
            Map.of(
                "empty", List.of("")
            ),
            request.getForm()
        );
    }

    /**
     * Tests a form value containing spaces and line breaks.
     */
    @Test
    void multilineValue() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"text\"\r\n"
                + "\r\n"
                + "first line\r\n"
                + "second line\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);

        assertEquals(
            List.of("first line\r\nsecond line"),
            request.getForm().get("text")
        );
    }

    /**
     * Tests that the original request body is preserved.
     */
    @Test
    void originalBody() throws ServerException {
        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--\r\n";

        final Request request = parse(body);

        assertArrayEquals(
            body.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            request.getBody().readAllBytes()
        );
    }

    /**
     * Tests that data after the final boundary remains unread.
     */
    @Test
    void dataAfterFinalBoundary() throws ServerException {
        final String tail = "GET /next HTTP/1.1\r\n";

        final String requestData =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n"
                + "\r\n"
                + "Ivan\r\n"
                + "--" + BOUNDARY + "--"
                + tail;

        final ByteSource source = new StringByteSource(requestData);
        final MemoryUploadedData data = new MemoryUploadedData(
            bytes(requestData)
        );

        final RequestBuilder builder = new RequestBuilder()
            .setHeaders(headers());

        MultipartParser.parse(
            source,
            data,
            BOUNDARY,
            STANDARD_OPTIONS,
            builder
        );

        final StringBuilder remaining = new StringBuilder();
        int value;

        while ((value = source.read()) != -1) {
            remaining.append((char) value);
        }

        assertEquals(tail, remaining.toString());
    }

    /**
     * Tests an unknown file content type.
     */
    @Test
    void unknownContentType() throws ServerException {
        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"data.bin\"\r\n"
                + "Content-Type: application/x-strange-thing\r\n"
                + "\r\n"
                + "data\r\n"
                + "--" + BOUNDARY + "--"
        );

        final UploadedFile file =
            request.getFiles().get("file").getFirst();

        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            file.getContentType()
        );
    }

    /**
     * Tests a file without a Content-Type header.
     */
    @Test
    void missingContentType() throws ServerException {
        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"data.bin\"\r\n"
                + "\r\n"
                + "data\r\n"
                + "--" + BOUNDARY + "--"
        );

        final UploadedFile file =
            request.getFiles().get("file").getFirst();

        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            file.getContentType()
        );
    }

    /**
     * Tests that unknown part headers are ignored.
     */
    @Test
    void unknownHeaders() throws ServerException {
        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "X-Something: one\r\n"
                + "Another-Header: two\r\n"
                + "Content-Disposition: form-data; name=\"field\"\r\n"
                + "\r\n"
                + "value\r\n"
                + "--" + BOUNDARY + "--"
        );

        assertEquals(
            List.of("value"),
            request.getForm().get("field")
        );
    }

    /**
     * Tests Content-Disposition parameters in a different order.
     */
    @Test
    void dispositionParameterOrder() throws ServerException {
        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "filename=\"data.bin\"; name=\"file\"\r\n"
                + "\r\n"
                + "data\r\n"
                + "--" + BOUNDARY + "--"
        );

        final UploadedFile file =
            request.getFiles().get("file").getFirst();

        assertEquals("data.bin", file.getName());
        assertArrayEquals(bytes("data"), file.readAllBytes());
    }

    /**
     * Tests unquoted Content-Disposition parameters.
     */
    @Test
    void unquotedDispositionParameters() throws ServerException {
        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=file; filename=data.bin\r\n"
                + "\r\n"
                + "data\r\n"
                + "--" + BOUNDARY + "--"
        );

        final UploadedFile file =
            request.getFiles().get("file").getFirst();

        assertEquals("data.bin", file.getName());
        assertArrayEquals(bytes("data"), file.readAllBytes());
    }

    /**
     * Tests optional whitespace around disposition parameters.
     */
    @Test
    void dispositionWhitespace() throws ServerException {
        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data ; "
                + " name = \"field\" \r\n"
                + "\r\n"
                + "value\r\n"
                + "--" + BOUNDARY + "--"
        );

        assertEquals(
            List.of("value"),
            request.getForm().get("field")
        );
    }

    /**
     * Tests a final boundary without trailing CRLF.
     */
    @Test
    void finalBoundaryWithoutCrlf() throws ServerException {
        final Request request = parse(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"field\"\r\n"
                + "\r\n"
                + "value\r\n"
                + "--" + BOUNDARY + "--"
        );

        assertEquals(
            List.of("value"),
            request.getForm().get("field")
        );
    }
}
