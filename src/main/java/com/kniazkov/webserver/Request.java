/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.List;
import java.util.Map;

/**
 * Represents an HTTP request.
 * <p>
 * Implementations of this interface are immutable. All collections returned
 * by this interface are immutable.
 */
public interface Request {

    /**
     * Returns the request headers.
     *
     * @return
     *     the request headers.
     */
    RequestHeaders getHeaders();

    /**
     * Returns the parsed request path.
     *
     * @return
     *     the request path.
     */
    RequestPath getPath();

    /**
     * Returns parameters obtained from the request target query string.
     *
     * @return
     *     an immutable map containing immutable lists of query parameter
     *     values.
     */
    Map<String, List<String>> getQuery();

    /**
     * Returns form parameters obtained from the request body.
     * <p>
     * For GET requests, this map is empty.
     *
     * @return
     *     an immutable map containing immutable lists of form parameter
     *     values.
     */
    Map<String, List<String>> getForm();

    /**
     * Returns files uploaded as part of the request.
     *
     * @return
     *     an immutable map containing immutable lists of uploaded files.
     */
    Map<String, List<UploadedFile>> getFiles();

    /**
     * Returns cookies supplied with the request.
     *
     * @return
     *     an immutable map of cookie names to values.
     */
    Map<String, String> getCookies();

    /**
     * Returns the original request body.
     * <p>
     * The data remains available only while the request handler is running.
     *
     * @return
     *     the uploaded request data.
     */
    UploadedData getBody();
}
