/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests serialization of configurable responses.
 */
final class ResponseSerializerTest {

    /**
     * Tests serializing a selected Content-Type and raw bytes.
     */
    @Test
    void selectedContentType() throws Exception {
        final Response response = new ResponseFactoryImpl()
            .custom(
                HttpStatus.CREATED,
                ContentType.APPLICATION_GEO_JSON,
                new byte[]{0, (byte) 0xff}
            )
            .build();

        final byte[] serialized = ResponseSerializer.serialize(
            response,
            HttpVersion.HTTP_1_1,
            false
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
                "Content-Type: application/geo+json\r\n"
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
            .custom(
                HttpStatus.NO_CONTENT,
                ContentType.APPLICATION_OCTET_STREAM,
                new byte[0]
            )
            .build();

        final String serialized = new String(
            ResponseSerializer.serialize(
                response,
                HttpVersion.HTTP_1_1,
                false
            ),
            StandardCharsets.ISO_8859_1
        );

        assertTrue(serialized.startsWith("HTTP/1.1 204 No Content"));
        assertFalse(serialized.contains("Content-Type:"));
        assertFalse(serialized.contains("Content-Length:"));
        assertTrue(serialized.endsWith("\r\n\r\n"));
    }

    /**
     * Tests declaring the character set of factory-generated UTF-8 text.
     */
    @Test
    void utf8Charset() throws Exception {
        final Response response = new ResponseFactoryImpl()
            .fromText("Hello")
            .build();

        final String serialized = new String(
            ResponseSerializer.serialize(
                response,
                HttpVersion.HTTP_1_1,
                true
            ),
            StandardCharsets.ISO_8859_1
        );

        assertTrue(
            serialized.contains(
                "Content-Type: text/plain; charset=UTF-8\r\n"
            )
        );
        assertFalse(serialized.contains("Connection:"));
    }

    /**
     * Tests protocol-version-specific connection headers.
     */
    @Test
    void connectionHeaders() throws Exception {
        final Response response = new ResponseFactoryImpl()
            .fromText("Hello")
            .build();

        assertTrue(
            serialize(response, HttpVersion.HTTP_1_1, false)
                .contains("Connection: close\r\n")
        );
        assertFalse(
            serialize(response, HttpVersion.HTTP_1_1, true)
                .contains("Connection:")
        );
        assertTrue(
            serialize(response, HttpVersion.HTTP_1_0, true)
                .contains("Connection: keep-alive\r\n")
        );
        assertTrue(
            serialize(response, HttpVersion.HTTP_1_0, false)
                .contains("Connection: close\r\n")
        );
    }

    /**
     * Tests that transport-owned headers are ignored defensively during
     * serialization even for a custom response implementation.
     */
    @Test
    void ignoresTransportHeaders() throws Exception {
        final Response response = new ResponseImpl(
            HttpStatus.OK,
            ContentType.TEXT_PLAIN,
            Map.of(
                "Transfer-Encoding",
                List.of("chunked"),
                "Connection",
                List.of("upgrade")
            ),
            "Hello".getBytes(StandardCharsets.UTF_8)
        );

        final String serialized = serialize(
            response,
            HttpVersion.HTTP_1_1,
            false
        );

        assertFalse(serialized.contains("Transfer-Encoding:"));
        assertFalse(serialized.contains("Connection: upgrade"));
        assertTrue(serialized.contains("Connection: close\r\n"));
        assertTrue(serialized.contains("Content-Length: 5\r\n"));
    }

    /**
     * Tests validation as a final safeguard for custom response
     * implementations that bypass the response builder.
     */
    @Test
    void rejectsUnsafeCustomHeader() {
        final Response response = new ResponseImpl(
            HttpStatus.OK,
            ContentType.TEXT_PLAIN,
            Map.of(
                "X-Test",
                List.of("before\0after")
            ),
            new byte[0]
        );

        assertThrows(
            ServerException.class,
            () -> ResponseSerializer.serialize(
                response,
                HttpVersion.HTTP_1_1,
                true
            )
        );
    }

    /**
     * Serializes a response as ISO-8859-1 text for header assertions.
     *
     * @param response
     *     the response.
     * @param version
     *     the HTTP version.
     * @param keepAlive
     *     whether the connection remains persistent.
     * @return
     *     the serialized response.
     * @throws Exception
     *     if serialization fails.
     */
    private static String serialize(
        final Response response,
        final HttpVersion version,
        final boolean keepAlive
    ) throws Exception {
        return new String(
            ResponseSerializer.serialize(
                response,
                version,
                keepAlive
            ),
            StandardCharsets.ISO_8859_1
        );
    }
}
