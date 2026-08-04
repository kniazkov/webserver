/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a file uploaded as part of an HTTP request.
 * <p>
 * This class is immutable. The uploaded file data is copied when the object
 * is created and whenever it is returned to the caller.
 */
public final class UploadedFile {

    /**
     * The original file name supplied by the client.
     */
    private final String fileName;

    /**
     * The content type supplied by the client.
     */
    private final String contentType;

    /**
     * The uploaded file data.
     */
    private final byte[] data;

    /**
     * Creates an uploaded file.
     *
     * @param fileName
     *     the original file name supplied by the client.
     * @param contentType
     *     the content type supplied by the client.
     * @param data
     *     the uploaded file data.
     * @throws NullPointerException
     *     if any argument is {@code null}.
     */
    public UploadedFile(
        final String fileName,
        final String contentType,
        final byte[] data
    ) {
        this.fileName = Objects.requireNonNull(
            fileName,
            "File name must not be null."
        );
        this.contentType = Objects.requireNonNull(
            contentType,
            "Content type must not be null."
        );
        this.data = Arrays.copyOf(
            Objects.requireNonNull(data, "File data must not be null."),
            data.length
        );
    }

    /**
     * Returns the original file name supplied by the client.
     *
     * @return
     *     the original file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the content type supplied by the client.
     *
     * @return
     *     the content type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns a copy of the uploaded file data.
     *
     * @return
     *     a copy of the file data.
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Returns the size of the uploaded file in bytes.
     *
     * @return
     *     the file size in bytes.
     */
    public int getSize() {
        return data.length;
    }
}
