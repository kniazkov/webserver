/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.ServerException;

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
            request.getBody()
        );
    }
}
