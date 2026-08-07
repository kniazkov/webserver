/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.RequestHeaders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable implementation of HTTP request headers.
 */
final class RequestHeadersImpl implements RequestHeaders {

    /**
     * The HTTP method.
     */
    private final HttpMethod method;

    /**
     * The request target.
     */
    private final String target;

    /**
     * The HTTP protocol version.
     */
    private final HttpVersion version;

    /**
     * The HTTP header values.
     */
    private final Map<String, List<String>> values;

    /**
     * Creates HTTP request headers.
     *
     * @param method
     *     the HTTP method.
     * @param target
     *     the request target.
     * @param version
     *     the HTTP protocol version.
     * @param values
     *     the HTTP header values.
     */
    RequestHeadersImpl(
        final HttpMethod method,
        final String target,
        final HttpVersion version,
        final Map<String, List<String>> values
    ) {
        this.method = method;
        this.target = target;
        this.version = version;

        final Map<String, List<String>> copy = new LinkedHashMap<>();
        values.forEach(
            (name, list) -> copy.put(name, List.copyOf(list))
        );
        this.values = Map.copyOf(copy);
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public HttpVersion getVersion() {
        return version;
    }

    @Override
    public Map<String, List<String>> getValues() {
        return values;
    }
}
