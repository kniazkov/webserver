/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.ServerException;

/**
 * Base class for request headers parser tests.
 */
abstract class RequestHeadersParserBaseTest {

    /**
     * Parses request headers from the specified string.
     *
     * @param value
     *     the HTTP request.
     * @return
     *     the parsed request headers.
     * @throws ServerException
     *     if the request headers are invalid.
     */
    protected static RequestHeaders parse(final String value)
            throws ServerException {
        final ByteSource byteSource = new StringByteSource(value);
        final StringSource stringSource = new StringSource(byteSource);

        return RequestHeadersParser.parse(stringSource);
    }
}
