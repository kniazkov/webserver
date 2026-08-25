/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.RequestPath;
import com.kniazkov.webserver.ServerException;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

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
     * @param value
     *     the original path.
     * @return
     *     the parsed request path.
     * @throws ServerException
     *     if the path is invalid.
     */
    static RequestPath build(final String value) throws ServerException {
        if (value == null) {
            throw new ServerException("Path must not be null");
        }

        final String path = decode(value);

        if (path.equals("/")) {
            return RootRequestPath.getInstance();
        }

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
     * Decodes and validates a request path.
     *
     * @param value
     *     the encoded path.
     * @return
     *     the decoded canonical path.
     * @throws ServerException
     *     if the path is invalid.
     */
    private static String decode(final String value)
        throws ServerException {
        if (value.isEmpty() || value.charAt(0) != '/') {
            throw new ServerException(
                "Path must start with '/'"
            );
        }

        final StringBuilder result = new StringBuilder();
        result.append('/');

        int start = 1;

        while (start <= value.length()) {
            final int slash = value.indexOf('/', start);
            final boolean last = slash < 0;
            final int end = last ? value.length() : slash;

            if (end == start && !last) {
                throw new ServerException("Empty path segment");
            }

            final String segment = decodeSegment(
                value.substring(start, end)
            );

            validateSegment(segment);
            result.append(segment);

            if (last) {
                return result.toString();
            }

            result.append('/');
            start = slash + 1;
        }

        throw new IllegalStateException("Request path decoding failed");
    }

    /**
     * Percent-decodes one path segment using strict UTF-8.
     *
     * @param value
     *     the encoded segment.
     * @return
     *     the decoded segment.
     * @throws ServerException
     *     if the segment contains invalid path syntax or UTF-8.
     */
    private static String decodeSegment(final String value)
        throws ServerException {
        final byte[] data = new byte[value.length()];
        int size = 0;

        for (int index = 0; index < value.length(); index++) {
            final char ch = value.charAt(index);

            if (ch == '%') {
                if (index + 2 >= value.length()) {
                    throw new ServerException(
                        "Invalid path percent encoding"
                    );
                }

                final int high = hex(value.charAt(++index));
                final int low = hex(value.charAt(++index));

                if (high < 0 || low < 0) {
                    throw new ServerException(
                        "Invalid path percent encoding"
                    );
                }

                data[size++] = (byte) ((high << 4) | low);
            } else {
                if (ch > 0x7f || !isPathCharacter(ch)) {
                    throw new ServerException(
                        "Invalid request path character"
                    );
                }

                data[size++] = (byte) ch;
            }
        }

        try {
            return StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(data, 0, size))
                .toString();
        } catch (CharacterCodingException exception) {
            throw new ServerException(
                "Invalid UTF-8 path encoding",
                exception
            );
        }
    }

    /**
     * Validates a decoded path segment.
     *
     * @param segment
     *     the decoded segment.
     * @throws ServerException
     *     if the segment is ambiguous or unsafe.
     */
    private static void validateSegment(final String segment)
        throws ServerException {
        if (segment.equals(".") || segment.equals("..")) {
            throw new ServerException(
                "Invalid path segment: " + segment
            );
        }

        for (int offset = 0; offset < segment.length();) {
            final int codePoint = segment.codePointAt(offset);

            if (
                codePoint == '/'
                    || codePoint == '\\'
                    || Character.isISOControl(codePoint)
            ) {
                throw new ServerException(
                    "Invalid decoded path character"
                );
            }

            offset += Character.charCount(codePoint);
        }
    }

    /**
     * Returns whether a character is allowed literally in a URI path segment.
     *
     * @param ch
     *     the character.
     * @return
     *     {@code true} if the character belongs to the URI {@code pchar}
     *     grammar, excluding percent encoding.
     */
    private static boolean isPathCharacter(final char ch) {
        return ch >= '0' && ch <= '9'
            || ch >= 'A' && ch <= 'Z'
            || ch >= 'a' && ch <= 'z'
            || "-._~!$&'()*+,;=:@".indexOf(ch) >= 0;
    }

    /**
     * Converts a hexadecimal character to its numeric value.
     *
     * @param ch
     *     the character.
     * @return
     *     the numeric value, or {@code -1} if the character is invalid.
     */
    private static int hex(final char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }

        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }

        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }

        return -1;
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
