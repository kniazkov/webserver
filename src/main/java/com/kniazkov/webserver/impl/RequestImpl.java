/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.RequestPath;
import com.kniazkov.webserver.UploadedFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable implementation of an HTTP request.
 */
final class RequestImpl implements Request {

    /**
     * The request headers.
     */
    private final RequestHeaders headers;

    /**
     * The request path.
     */
    private final RequestPath path;

    /**
     * The query parameters.
     */
    private final Map<String, List<String>> query;

    /**
     * The form parameters.
     */
    private final Map<String, List<String>> form;

    /**
     * The uploaded files.
     */
    private final Map<String, List<UploadedFile>> files;

    /**
     * The cookies.
     */
    private final Map<String, String> cookies;

    /**
     * The original request body.
     */
    private final byte[] body;

    /**
     * Creates an HTTP request.
     *
     * @param headers
     *     the request headers.
     * @param path
     *     the request path.
     * @param query
     *     the query parameters.
     * @param form
     *     the form parameters.
     * @param files
     *     the uploaded files.
     * @param cookies
     *     the cookies.
     * @param body
     *     the original request body.
     */
    RequestImpl(
        final RequestHeaders headers,
        final RequestPath path,
        final Map<String, List<String>> query,
        final Map<String, List<String>> form,
        final Map<String, List<UploadedFile>> files,
        final Map<String, String> cookies,
        final byte[] body
    ) {
        this.headers = headers;
        this.path = path;
        this.query = copyLists(query);
        this.form = copyLists(form);
        this.files = copyLists(files);
        this.cookies = Map.copyOf(cookies);
        this.body = body.clone();
    }

    @Override
    public RequestHeaders getHeaders() {
        return headers;
    }

    @Override
    public RequestPath getPath() {
        return path;
    }

    @Override
    public Map<String, List<String>> getQuery() {
        return query;
    }

    @Override
    public Map<String, List<String>> getForm() {
        return form;
    }

    @Override
    public Map<String, List<UploadedFile>> getFiles() {
        return files;
    }

    @Override
    public Map<String, String> getCookies() {
        return cookies;
    }

    @Override
    public byte[] getBody() {
        return body.clone();
    }

    /**
     * Creates an immutable copy of a map containing lists.
     *
     * @param source
     *     the source map.
     * @param <T>
     *     the list item type.
     * @return
     *     the immutable copy.
     */
    private static <T> Map<String, List<T>> copyLists(final Map<String, List<T>> source) {
        final Map<String, List<T>> copy = new LinkedHashMap<>();
        source.forEach(
            (name, values) -> copy.put(name, List.copyOf(values))
        );
        return Map.copyOf(copy);
    }
}
