/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Represents a file uploaded as part of an HTTP request.
 * <p>
 * Implementations of this interface are immutable. Uploaded file data remains
 * available only while the request handler is running.
 */
public interface UploadedFile extends UploadedData {

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

}
