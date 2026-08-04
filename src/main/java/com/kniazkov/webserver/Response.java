/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.List;
import java.util.Map;

/**
 * Represents an HTTP response produced by an application.
 * <p>
 * The response contains the status, content type, additional header fields,
 * and response body. A separate serializer is responsible for converting this
 * object into a valid HTTP message.
 */
public interface Response {

    /**
     * Returns the HTTP response status.
     *
     * @return
     *     the response status.
     */
    HttpStatus getStatus();

    /**
     * Returns the content type of the response body.
     *
     * @return
     *     the response content type.
     */
    ContentType getContentType();

    /**
     * Returns the additional HTTP response header fields.
     * <p>
     * The returned map should be immutable. Header names may contain multiple
     * values.
     *
     * @return
     *     the response header fields.
     */
    Map<String, List<String>> getHeaders();

    /**
     * Returns the response body data.
     * <p>
     * Implementations should return a defensive copy to prevent modification
     * of their internal state.
     *
     * @return
     *     the response body data.
     */
    byte[] getData();
}
