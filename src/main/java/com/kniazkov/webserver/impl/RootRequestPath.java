/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.RequestPath;

/**
 * Represents the root request path.
 */
final class RootRequestPath implements RequestPath {

    /**
     * The singleton instance.
     */
    private static final RequestPath INSTANCE = new RootRequestPath();

    /**
     * Prevents external instantiation.
     */
    private RootRequestPath() {
    }

    /**
     * Returns the singleton instance.
     *
     * @return
     *     the root request path.
     */
    static RequestPath getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return "/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDirectory() {
        return "/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileName() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileType() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentType getContentType() {
        return ContentType.APPLICATION_OCTET_STREAM;
    }
}
