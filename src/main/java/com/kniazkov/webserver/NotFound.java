/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Represents a shared HTTP 404 Not Found response.
 * <p>
 * This class does not store response state. All response values are fixed.
 */
final class NotFound implements Response {

    /**
     * The shared instance of this response.
     */
    private static final Response INSTANCE = new NotFound();

    /**
     * The response body data.
     */
    private static final byte[] DATA = "Not Found".getBytes(
        StandardCharsets.UTF_8
    );

    /**
     * Prevents direct instantiation.
     */
    private NotFound() {
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
     *     HTTP 404 Not Found.
     */
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
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
