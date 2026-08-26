/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.List;
import java.util.Map;

/**
 * Represents parsed HTTP request headers.
 */
public interface RequestHeaders {

    /**
     * Returns the HTTP method.
     *
     * @return
     *     the HTTP method.
     */
    HttpMethod getMethod();

    /**
     * Returns the request target.
     *
     * @return
     *     the request target.
     */
    String getTarget();

    /**
     * Returns the HTTP protocol version.
     *
     * @return
     *     the HTTP protocol version.
     */
    HttpVersion getVersion();

    /**
     * Returns the HTTP header values.
     *
     * @return
     *     an immutable map containing immutable lists of header values.
     */
    Map<String, List<String>> getValues();
}
