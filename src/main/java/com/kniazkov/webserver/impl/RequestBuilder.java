/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.RequestPath;
import com.kniazkov.webserver.ServerException;
import com.kniazkov.webserver.UploadedFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds HTTP requests.
 */
final class RequestBuilder {

    /**
     * The request headers.
     */
    private RequestHeaders headers;

    /**
     * The request path.
     */
    private RequestPath path;

    /**
     * The query parameters.
     */
    private final Map<String, List<String>> query = new LinkedHashMap<>();

    /**
     * The form parameters.
     */
    private final Map<String, List<String>> form = new LinkedHashMap<>();

    /**
     * The uploaded files.
     */
    private final Map<String, List<UploadedFile>> files = new LinkedHashMap<>();

    /**
     * The cookies.
     */
    private final Map<String, String> cookies = new LinkedHashMap<>();

    /**
     * The original request body.
     */
    private StoredUploadedData body =
        new MemoryUploadedData(new byte[0]);

    /**
     * Sets the request headers.
     *
     * @param value
     *     the request headers.
     * @return
     *     this builder.
     */
    RequestBuilder setHeaders(final RequestHeaders value) {
        headers = value;
        return this;
    }

    /**
     * Sets the request path.
     *
     * @param value
     *     the request path.
     * @return
     *     this builder.
     */
    RequestBuilder setPath(final RequestPath value) {
        path = value;
        return this;
    }

    /**
     * Adds a query parameter value.
     *
     * @param name
     *     the parameter name.
     * @param value
     *     the parameter value.
     * @return
     *     this builder.
     */
    RequestBuilder addQuery(final String name, final String value) {
        query.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Adds a form parameter value.
     *
     * @param name
     *     the parameter name.
     * @param value
     *     the parameter value.
     * @return
     *     this builder.
     */
    RequestBuilder addForm(final String name, final String value) {
        form.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Adds an uploaded file.
     *
     * @param name
     *     the form field name.
     * @param value
     *     the uploaded file.
     * @return
     *     this builder.
     */
    RequestBuilder addFile(final String name, final UploadedFile value) {
        files.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Sets a cookie.
     *
     * @param name
     *     the cookie name.
     * @param value
     *     the cookie value.
     * @return
     *     this builder.
     */
    RequestBuilder setCookie(final String name, final String value) {
        cookies.put(name, value);
        return this;
    }

    /**
     * Sets the original request body.
     *
     * @param value
     *     the request body.
     * @return
     *     this builder.
     */
    RequestBuilder setBody(final StoredUploadedData value) {
        body = value;
        return this;
    }

    /**
     * Builds an immutable HTTP request.
     *
     * @return
     *     the HTTP request.
     * @throws ServerException
     *     if mandatory request data is missing.
     */
    Request build() throws ServerException {
        if (headers == null) {
            throw new ServerException("Request headers are not specified");
        }
        if (path == null) {
            throw new ServerException("Request path is not specified");
        }

        return new RequestImpl(
            headers,
            path,
            query,
            form,
            files,
            cookies,
            body
        );
    }
}
