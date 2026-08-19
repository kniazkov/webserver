/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.Response;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Serializes HTTP responses.
 */
final class ResponseSerializer {

    /**
     * The carriage return and line feed sequence.
     */
    private static final byte[] CRLF = {
        '\r', '\n'
    };

    /**
     * Prevents instantiation.
     */
    private ResponseSerializer() {
    }

    /**
     * Serializes an HTTP response.
     * <p>
     * The {@code Content-Type} and {@code Content-Length} headers supplied
     * by the response are ignored. Their values are generated from the
     * response itself.
     *
     * @param response
     *     the response.
     * @param version
     *     the HTTP version.
     * @return
     *     the complete serialized HTTP response.
     */
    static byte[] serialize(
        final Response response,
        final HttpVersion version
    ) {
        final byte[] data = response.getData();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        write(
            output,
            version.toString()
                + " "
                + response.getStatus().getCode()
                + " "
                + response.getStatus().getReason()
        );
        writeCrlf(output);

        for (
            Map.Entry<String, List<String>> entry
            : response.getHeaders().entrySet()
        ) {
            final String name = entry.getKey();

            if (
                name.equalsIgnoreCase("Content-Type")
                    || name.equalsIgnoreCase("Content-Length")
            ) {
                continue;
            }

            for (String value : entry.getValue()) {
                writeHeader(
                    output,
                    name,
                    value
                );
            }
        }

        if (response.getStatus().allowsBody()) {
            writeHeader(
                output,
                "Content-Type",
                response.getContentTypeValue()
            );

            writeHeader(
                output,
                "Content-Length",
                Integer.toString(data.length)
            );
        }

        writeCrlf(output);

        if (response.getStatus().allowsBody()) {
            output.writeBytes(data);
        }

        return output.toByteArray();
    }

    /**
     * Writes one HTTP header.
     *
     * @param output
     *     the output stream.
     * @param name
     *     the header name.
     * @param value
     *     the header value.
     */
    private static void writeHeader(
        final ByteArrayOutputStream output,
        final String name,
        final String value
    ) {
        write(output, name);
        write(output, ": ");
        write(output, value);
        writeCrlf(output);
    }

    /**
     * Writes ASCII text.
     *
     * @param output
     *     the output stream.
     * @param value
     *     the text.
     */
    private static void write(
        final ByteArrayOutputStream output,
        final String value
    ) {
        output.writeBytes(
            value.getBytes(StandardCharsets.US_ASCII)
        );
    }

    /**
     * Writes CRLF.
     *
     * @param output
     *     the output stream.
     */
    private static void writeCrlf(
        final ByteArrayOutputStream output
    ) {
        output.writeBytes(CRLF);
    }
}
