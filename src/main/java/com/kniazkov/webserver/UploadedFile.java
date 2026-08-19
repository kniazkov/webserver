/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Represents a file uploaded as part of an HTTP request.
 * <p>
 * Implementations of this interface are immutable.
 */
public interface UploadedFile {

    /**
     * Returns the original file name.
     *
     * @return
     *     the file name.
     */
    String getName();

    /**
     * Returns the content type of the uploaded file.
     *
     * @return
     *     the content type.
     */
    ContentType getContentType();

    /**
     * Returns the uploaded file data.
     * <p>
     * A new copy of the underlying byte array is created on every invocation.
     * Modifying the returned array does not affect this uploaded file.
     *
     * @return
     *     a copy of the uploaded file data.
     */
    byte[] getData();
}
