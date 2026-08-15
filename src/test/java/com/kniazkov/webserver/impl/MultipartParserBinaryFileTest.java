/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.ServerException;
import com.kniazkov.webserver.UploadedFile;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests parsing binary uploaded files from multipart form data.
 */
final class MultipartParserBinaryFileTest extends MultipartParserBaseTest {

    /**
     * Tests a file containing arbitrary binary bytes.
     */
    @Test
    void binaryData() throws ServerException {
        final byte[] fileData = {
            0x00,
            0x01,
            0x02,
            0x7f,
            (byte) 0x80,
            (byte) 0xfe,
            (byte) 0xff
        };

        final Request request = parse(
            multipartBody(
                "file",
                "data.bin",
                "application/octet-stream",
                fileData
            )
        );

        final UploadedFile file =
            request.getFiles().get("file").getFirst();

        assertEquals("data.bin", file.getName());
        assertArrayEquals(fileData, file.getData());
    }

    /**
     * Tests a file containing CRLF sequences.
     */
    @Test
    void crlfInsideFile() throws ServerException {
        final byte[] fileData = bytes(
            "first\r\n"
                + "second\r\n"
                + "\r\n"
                + "third"
        );

        final Request request = parse(
            multipartBody(
                "file",
                "lines.bin",
                "application/octet-stream",
                fileData
            )
        );

        assertArrayEquals(
            fileData,
            request.getFiles().get("file").getFirst().getData()
        );
    }

    /**
     * Tests data containing the boundary text without its required prefix.
     */
    @Test
    void boundaryTextInsideFile() throws ServerException {
        final byte[] fileData = bytes(
            "before--"
                + BOUNDARY
                + "--after"
        );

        final Request request = parse(
            multipartBody(
                "file",
                "data.bin",
                "application/octet-stream",
                fileData
            )
        );

        assertArrayEquals(
            fileData,
            request.getFiles().get("file").getFirst().getData()
        );
    }

    /**
     * Tests data containing an almost matching multipart boundary.
     */
    @Test
    void almostBoundaryInsideFile() throws ServerException {
        final byte[] fileData = bytes(
            "before\r\n--"
                + BOUNDARY
                + "Xafter"
        );

        final Request request = parse(
            multipartBody(
                "file",
                "data.bin",
                "application/octet-stream",
                fileData
            )
        );

        assertArrayEquals(
            fileData,
            request.getFiles().get("file").getFirst().getData()
        );
    }

    /**
     * Tests data where a boundary-like sequence appears several times.
     */
    @Test
    void severalAlmostBoundaries() throws ServerException {
        final byte[] fileData = bytes(
            "a\r\n--"
                + BOUNDARY
                + "Xa\r\n--"
                + BOUNDARY
                + "Yb\r\n--"
                + BOUNDARY
                + "Zc"
        );

        final Request request = parse(
            multipartBody(
                "file",
                "data.bin",
                "application/octet-stream",
                fileData
            )
        );

        assertArrayEquals(
            fileData,
            request.getFiles().get("file").getFirst().getData()
        );
    }

    /**
     * Tests binary data spanning several accumulator chunks.
     */
    @Test
    void largeBinaryFile() throws ServerException {
        final byte[] fileData = new byte[4097];

        for (int index = 0; index < fileData.length; index++) {
            fileData[index] = (byte) index;
        }

        final Request request = parse(
            multipartBody(
                "file",
                "large.bin",
                "application/octet-stream",
                fileData
            )
        );

        assertArrayEquals(
            fileData,
            request.getFiles().get("file").getFirst().getData()
        );
    }

    /**
     * Tests a boundary crossing an internal accumulator chunk boundary.
     */
    @Test
    void boundaryAcrossChunkBoundary() throws ServerException {
        final byte[] fileData = new byte[1019];

        for (int index = 0; index < fileData.length; index++) {
            fileData[index] = 'x';
        }

        final Request request = parse(
            multipartBody(
                "file",
                "boundary.bin",
                "application/octet-stream",
                fileData
            )
        );

        assertArrayEquals(
            fileData,
            request.getFiles().get("file").getFirst().getData()
        );
    }

    /**
     * Creates a complete multipart body containing one binary file.
     *
     * @param field
     *     the form field name.
     * @param filename
     *     the file name.
     * @param contentType
     *     the content type.
     * @param data
     *     the file data.
     * @return
     *     the multipart body.
     */
    private static byte[] multipartBody(
        final String field,
        final String filename,
        final String contentType,
        final byte[] data
    ) {
        final byte[] prefix = bytes(
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"" + field + "\"; "
                + "filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "\r\n"
        );

        final byte[] suffix = bytes(
            "\r\n--" + BOUNDARY + "--\r\n"
        );

        final byte[] result =
            new byte[prefix.length + data.length + suffix.length];

        System.arraycopy(
            prefix,
            0,
            result,
            0,
            prefix.length
        );

        System.arraycopy(
            data,
            0,
            result,
            prefix.length,
            data.length
        );

        System.arraycopy(
            suffix,
            0,
            result,
            prefix.length + data.length,
            suffix.length
        );

        return result;
    }
}
