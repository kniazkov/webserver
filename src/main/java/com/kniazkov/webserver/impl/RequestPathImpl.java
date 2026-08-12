/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.RequestPath;
import com.kniazkov.webserver.ServerException;

/**
 * Default implementation of {@link RequestPath}.
 */
final class RequestPathImpl implements RequestPath {

    /**
     * The complete path.
     */
    private final String path;

    /**
     * The directory path.
     */
    private final String directory;

    /**
     * The file name.
     */
    private final String fileName;

    /**
     * The file type.
     */
    private final String fileType;

    /**
     * The content type.
     */
    private final ContentType contentType;

    /**
     * Creates a request path.
     *
     * @param path
     *     the complete path.
     * @param directory
     *     the directory path.
     * @param fileName
     *     the file name.
     * @param fileType
     *     the file type.
     */
    private RequestPathImpl(
        final String path,
        final String directory,
        final String fileName,
        final String fileType
    ) {
        this.path = path;
        this.directory = directory;
        this.fileName = fileName;
        this.fileType = fileType;
        contentType = ContentType.fromExtension(fileType);
    }

    /**
     * Builds a request path.
     *
     * @param path
     *     the original path.
     * @return
     *     the parsed request path.
     * @throws ServerException
     *     if the path is invalid.
     */
    static RequestPath build(final String path) throws ServerException {
        if (path == null) {
            throw new ServerException("Path must not be null");
        }

        if (path.equals("/")) {
            return RootRequestPath.getInstance();
        }

        validate(path);

        final int slash = path.lastIndexOf('/');

        final String directory = path.substring(0, slash + 1);
        final String fileName = path.substring(slash + 1);
        final String fileType = getFileType(fileName);

        return new RequestPathImpl(
            path,
            directory,
            fileName,
            fileType
        );
    }

    /**
     * Validates a request path.
     *
     * @param path
     *     the path.
     * @throws ServerException
     *     if the path is invalid.
     */
    private static void validate(final String path) throws ServerException {
        if (path.isEmpty() || path.charAt(0) != '/') {
            throw new ServerException(
                "Path must start with '/'"
            );
        }

        if (
            path.indexOf('?') >= 0
                || path.indexOf('#') >= 0
                || path.indexOf('\\') >= 0
        ) {
            throw new ServerException(
                "Invalid request path: " + path
            );
        }

        if (path.endsWith("/")) {
            throw new ServerException(
                "Path must end with a file name"
            );
        }

        int start = 1;

        while (start < path.length()) {
            final int slash = path.indexOf('/', start);
            final int end = slash < 0 ? path.length() : slash;

            if (end == start) {
                throw new ServerException(
                    "Empty path segment"
                );
            }

            final String segment = path.substring(start, end);

            if (segment.equals(".") || segment.equals("..")) {
                throw new ServerException(
                    "Invalid path segment: " + segment
                );
            }

            if (slash < 0) {
                return;
            }

            start = slash + 1;
        }
    }

    /**
     * Extracts the file type from a file name.
     *
     * @param fileName
     *     the file name.
     * @return
     *     the file type, or an empty string.
     */
    private static String getFileType(final String fileName) {
        final int dot = fileName.lastIndexOf('.');

        if (dot <= 0 || dot == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dot + 1).toLowerCase();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDirectory() {
        return directory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileName() {
        return fileName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileType() {
        return fileType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentType getContentType() {
        return contentType;
    }
}
