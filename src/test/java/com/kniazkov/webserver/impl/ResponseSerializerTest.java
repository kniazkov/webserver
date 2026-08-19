/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.Response;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests serialization of configurable responses.
 */
final class ResponseSerializerTest {

    /**
     * Tests preserving a custom Content-Type value and raw bytes.
     */
    @Test
    void customContentType() throws Exception {
        final Response response = new ResponseFactoryImpl()
            .fromBytes(
                new byte[]{0, (byte) 0xff},
                "application/vnd.example; version=2"
            )
            .setStatus(HttpStatus.CREATED)
            .build();

        final byte[] serialized = ResponseSerializer.serialize(
            response,
            HttpVersion.HTTP_1_1
        );

        final String headers = new String(
            serialized,
            0,
            serialized.length - 2,
            StandardCharsets.ISO_8859_1
        );

        assertTrue(headers.startsWith("HTTP/1.1 201 Created\r\n"));
        assertTrue(
            headers.contains(
                "Content-Type: application/vnd.example; version=2\r\n"
            )
        );
        assertTrue(headers.contains("Content-Length: 2\r\n"));
        assertEquals(0, serialized[serialized.length - 2]);
        assertEquals((byte) 0xff, serialized[serialized.length - 1]);
    }

    /**
     * Tests that a 204 response has neither body nor entity headers.
     */
    @Test
    void noContent() throws Exception {
        final Response response = new ResponseFactoryImpl()
            .response(HttpStatus.NO_CONTENT)
            .build();

        final String serialized = new String(
            ResponseSerializer.serialize(
                response,
                HttpVersion.HTTP_1_1
            ),
            StandardCharsets.ISO_8859_1
        );

        assertTrue(serialized.startsWith("HTTP/1.1 204 No Content"));
        assertFalse(serialized.contains("Content-Type:"));
        assertFalse(serialized.contains("Content-Length:"));
        assertTrue(serialized.endsWith("\r\n\r\n"));
    }
}
