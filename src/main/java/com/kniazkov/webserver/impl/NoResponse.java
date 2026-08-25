/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Response;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the absence of an explicit HTTP response.
 * <p>
 * When this response is returned by a handler, the server applies its default
 * response algorithm, such as attempting to serve a static file corresponding
 * to the request path.
 */
final class NoResponse implements Response {

    /**
     * The singleton instance.
     */
    private static final Response INSTANCE = new NoResponse();

    /**
     * Prevents external instantiation.
     */
    private NoResponse() {
    }

    /**
     * Returns the singleton instance.
     *
     * @return
     *     the no-response instance.
     */
    static Response getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException
     *     always, because this object does not represent an HTTP response.
     */
    @Override
    public HttpStatus getStatus() {
        throw invalidState();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException
     *     always, because this object does not represent an HTTP response.
     */
    @Override
    public ContentType getContentType() {
        throw invalidState();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException
     *     always, because this object does not represent an HTTP response.
     */
    @Override
    public Optional<Charset> getCharset() {
        throw invalidState();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException
     *     always, because this object does not represent an HTTP response.
     */
    @Override
    public Map<String, List<String>> getHeaders() {
        throw invalidState();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException
     *     always, because this object does not represent an HTTP response.
     */
    @Override
    public byte[] getData() {
        throw invalidState();
    }

    /**
     * Creates an exception indicating that response data is unavailable.
     *
     * @return
     *     the exception.
     */
    private static IllegalStateException invalidState() {
        return new IllegalStateException(
            "NoResponse does not contain HTTP response data"
        );
    }
}
