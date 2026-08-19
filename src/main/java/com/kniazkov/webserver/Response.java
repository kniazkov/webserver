/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.List;
import java.util.Map;

/**
 * Represents an HTTP response returned by a request handler.
 * <p>
 * Implementations of this interface are immutable. Collections returned by
 * this interface are also immutable.
 */
public interface Response {

    /**
     * Returns the HTTP status.
     *
     * @return
     *     the HTTP status.
     */
    HttpStatus getStatus();

    /**
     * Returns the content type.
     *
     * @return
     *     the content type.
     */
    ContentType getContentType();

    /**
     * Returns the response headers.
     * <p>
     * Both the returned map and the lists contained in it are immutable.
     *
     * @return
     *     the response headers.
     */
    Map<String, List<String>> getHeaders();

    /**
     * Returns the response body.
     * <p>
     * A new copy of the response data is created on every invocation.
     * Modifying the returned array does not affect this response.
     *
     * @return
     *     a copy of the response body.
     */
    byte[] getData();
}
