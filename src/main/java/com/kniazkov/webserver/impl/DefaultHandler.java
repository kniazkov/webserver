/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseFactory;

/**
 * Default request handler.
 * <p>
 * This handler does not process requests explicitly and always returns
 * {@link ResponseFactory#noResponse()}, allowing the server to apply its
 * default request processing algorithm.
 */
public final class DefaultHandler implements Handler {

    /**
     * The singleton instance.
     */
    private static final Handler INSTANCE = new DefaultHandler();

    /**
     * Prevents external instantiation.
     */
    private DefaultHandler() {
    }

    /**
     * Returns the default request handler.
     *
     * @return
     *     the default request handler.
     */
    public static Handler getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response handle(
        final Request request,
        final Environment environment
    ) {
        return environment
            .getResponseFactory()
            .noResponse();
    }
}
