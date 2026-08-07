/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.UploadedFile;

import java.util.Objects;

/**
 * Immutable implementation of an uploaded file.
 */
final class UploadedFileImpl implements UploadedFile {

    /**
     * The original file name.
     */
    private final String name;

    /**
     * The content type.
     */
    private final ContentType contentType;

    /**
     * The uploaded file data.
     */
    private final byte[] data;

    /**
     * Creates an uploaded file.
     *
     * @param name
     *     the original file name.
     * @param contentType
     *     the content type.
     * @param data
     *     the uploaded file data.
     * @throws NullPointerException
     *     if any argument is {@code null}.
     */
    UploadedFileImpl(
        final String name,
        final ContentType contentType,
        final byte[] data
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.contentType = Objects.requireNonNull(
            contentType,
            "contentType"
        );
        this.data = Objects.requireNonNull(data, "data").clone();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ContentType getContentType() {
        return contentType;
    }

    @Override
    public byte[] getData() {
        return data.clone();
    }
}
