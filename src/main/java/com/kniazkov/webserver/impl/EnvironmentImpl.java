/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Environment;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ResponseFactory;

import java.util.Objects;

/**
 * Default implementation of {@link Environment}.
 * <p>
 * An instance of this class represents the environment shared by request
 * handlers running within one web server. It provides access to server-level
 * services that handlers may use while processing requests.
 * <p>
 * The environment is created once when the server starts and can be safely
 * shared between workers because all objects exposed by it are expected to
 * be thread-safe or immutable.
 */
final class EnvironmentImpl implements Environment {

    /**
     * The response factory available to request handlers.
     */
    private final ResponseFactory responseFactory;

    /**
     * Creates the default handler environment using the specified server
     * options.
     *
     * @param options
     *     the server options.
     */
    EnvironmentImpl(final Options options) {
        Objects.requireNonNull(
            options,
            "Options must not be null"
        );

        responseFactory = new ResponseFactoryImpl(
            options.getErrorPage()
        );
    }

    /**
     * Returns the response factory available to request handlers.
     *
     * @return
     *     the response factory.
     */
    @Override
    public ResponseFactory getResponseFactory() {
        return responseFactory;
    }
}
