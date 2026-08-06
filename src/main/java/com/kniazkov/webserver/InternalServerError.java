/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Represents a shared HTTP 500 Internal Server Error response.
 * <p>
 * This class does not store response state. All response values are fixed.
 */
final class InternalServerError implements Response {

    /**
     * The shared instance of this response.
     */
    private static final Response INSTANCE = new InternalServerError();

    /**
     * The response body data.
     */
    private static final byte[] DATA = "Internal Server Error".getBytes(
        StandardCharsets.UTF_8
    );

    /**
     * Prevents direct instantiation.
     */
    private InternalServerError() {
    }

    /**
     * Returns the shared response instance.
     *
     * @return
     *     the shared response instance.
     */
    public static Response getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the HTTP response status.
     *
     * @return
     *     HTTP 500 Internal Server Error.
     */
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Returns the response content type.
     *
     * @return
     *     the plain text content type.
     */
    @Override
    public ContentType getContentType() {
        return ContentType.TEXT_PLAIN;
    }

    /**
     * Returns the additional response header fields.
     *
     * @return
     *     an empty immutable map.
     */
    @Override
    public Map<String, List<String>> getHeaders() {
        return Map.of();
    }

    /**
     * Returns a copy of the response body data.
     *
     * @return
     *     a copy of the response body data.
     */
    @Override
    public byte[] getData() {
        return DATA.clone();
    }
}
