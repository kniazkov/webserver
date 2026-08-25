/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.ServerException;

import java.nio.charset.StandardCharsets;

/**
 * Base class for multipart parser tests.
 */
abstract class MultipartParserBaseTest {

    /**
     * Test boundary.
     */
    protected static final String BOUNDARY = "test-boundary";

    /**
     * Test server options.
     */
    protected static final Options STANDARD_OPTIONS = new Options.Builder().build();

    /**
     * Parses multipart data.
     *
     * @param body
     *     the multipart body.
     * @return
     *     the resulting request.
     * @throws ServerException
     *     if parsing fails.
     */
    protected static Request parse(final String body)
            throws ServerException {
        return parse(body, STANDARD_OPTIONS);
    }

    /**
     * Parses multipart data.
     *
     * @param body
     *     the multipart body as byte array.
     * @return
     *     the resulting request.
     * @throws ServerException
     *     if parsing fails.
     */
    protected static Request parse(final byte[] body)
            throws ServerException {
        return parse(body, STANDARD_OPTIONS);
    }

    /**
     * Parses multipart data with specified options.
     *
     * @param body
     *     the multipart body.
     * @param options
     *     the parser options.
     * @return
     *     the resulting request.
     * @throws ServerException
     *     if parsing fails.
     */
    protected static Request parse(final String body, final Options options)
        throws ServerException {
        return parse(body, BOUNDARY, options);
    }

    /**
     * Parses multipart data with an explicit boundary and options.
     *
     * @param body
     *     the multipart body.
     * @param boundary
     *     the multipart boundary.
     * @param options
     *     the parser options.
     * @return
     *     the resulting request.
     * @throws ServerException
     *     if parsing fails.
     */
    protected static Request parse(
        final String body,
        final String boundary,
        final Options options
    ) throws ServerException {
        final MemoryUploadedData data = new MemoryUploadedData(
            body.getBytes(StandardCharsets.UTF_8)
        );

        final RequestBuilder builder = new RequestBuilder()
            .setHeaders(headers())
            .setPath(RootRequestPath.getInstance())
            .setBody(data);

        MultipartParser.parse(
            new StringByteSource(body),
            data,
            boundary,
            options,
            builder
        );

        return builder.build();
    }

    /**
     * Parses multipart data with specified options.
     *
     * @param body
     *     the multipart body as byte array.
     * @param options
     *     the parser options.
     * @return
     *     the resulting request.
     * @throws ServerException
     *     if parsing fails.
     */
    protected static Request parse(final byte[] body, final Options options)
        throws ServerException {
        final MemoryUploadedData data = new MemoryUploadedData(body);

        final RequestBuilder builder = new RequestBuilder()
            .setHeaders(headers())
            .setPath(RootRequestPath.getInstance())
            .setBody(data);

        MultipartParser.parse(
            new ByteArrayByteSource(body),
            data,
            BOUNDARY,
            options,
            builder
        );

        return builder.build();
    }

    /**
     * Returns test request headers.
     *
     * @return
     *     the request headers.
     * @throws ServerException
     *     if building fails.
     */
    protected static RequestHeaders headers() throws ServerException {
        return new RequestHeadersBuilder()
            .setMethod(HttpMethod.POST)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .build();
    }


    /**
     * Converts ASCII text to bytes.
     *
     * @param value
     *     the text.
     * @return
     *     the bytes.
     */
    protected static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
