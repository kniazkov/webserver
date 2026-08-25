/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Represents the path part of an HTTP request target.
 * <p>
 * Implementations of this interface are immutable.
 */
public interface RequestPath {

    /**
     * Returns the complete request path.
     * <p>
     * The returned path is percent-decoded using UTF-8, always starts with
     * {@code /}, and preserves a trailing slash.
     *
     * @return
     *     the complete request path.
     */
    String getPath();

    /**
     * Returns the directory part of the request path.
     * <p>
     * The returned path always starts with {@code /} and does not include
     * the file name. A request path ending in {@code /} is itself a directory
     * path.
     *
     * @return
     *     the directory path.
     */
    String getDirectory();

    /**
     * Returns the complete file name.
     *
     * @return
     *     the file name, or an empty string if the request path does not
     *     contain a file name.
     */
    String getFileName();

    /**
     * Returns the file type determined from the file name.
     * <p>
     * The type does not include the leading dot.
     *
     * @return
     *     the file type, or an empty string if the file name has no type.
     */
    String getFileType();

    /**
     * Returns the content type determined from the file type.
     *
     * @return
     *     the corresponding content type, or
     *     {@link ContentType#APPLICATION_OCTET_STREAM} if the file type
     *     is not recognized.
     */
    ContentType getContentType();
}
