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
    protected static final Options OPTIONS = new Options.Builder().build();

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
        final RequestBuilder builder = new RequestBuilder()
            .setHeaders(headers())
            .setBody(body.getBytes(StandardCharsets.UTF_8));

        MultipartParser.parse(
            new StringByteSource(body),
            BOUNDARY,
            OPTIONS,
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
    private static RequestHeaders headers() throws ServerException {
        return new RequestHeadersBuilder()
            .setMethod(HttpMethod.POST)
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1)
            .build();
    }
}
