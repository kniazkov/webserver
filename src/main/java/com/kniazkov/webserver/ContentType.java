/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.Locale;

/**
 * Represents a content type supported by the web server.
 */
public enum ContentType {

    /**
     * Plain text content.
     */
    TEXT_PLAIN("text/plain", "txt"),

    /**
     * HTML content.
     */
    TEXT_HTML("text/html", "html", "htm"),

    /**
     * CSS content.
     */
    TEXT_CSS("text/css", "css"),

    /**
     * JavaScript content.
     */
    TEXT_JAVASCRIPT("text/javascript", "js"),

    /**
     * JSON content.
     */
    APPLICATION_JSON("application/json", "json"),

    /**
     * XML application content.
     */
    APPLICATION_XML("application/xml", "xml"),

    /**
     * URL-encoded form content.
     */
    APPLICATION_FORM_URLENCODED(
        "application/x-www-form-urlencoded"
    ),

    /**
     * Multipart form content.
     */
    MULTIPART_FORM_DATA("multipart/form-data"),

    /**
     * Arbitrary binary content.
     * <p>
     * This value is also used when a supplied content type is not recognized.
     */
    APPLICATION_OCTET_STREAM("application/octet-stream", "bin"),

    /**
     * Portable document format content.
     */
    APPLICATION_PDF("application/pdf", "pdf"),

    /**
     * ZIP archive content.
     */
    APPLICATION_ZIP("application/zip", "zip"),

    /**
     * PNG image content.
     */
    IMAGE_PNG("image/png", "png"),

    /**
     * JPEG image content.
     */
    IMAGE_JPEG("image/jpeg", "jpg", "jpeg"),

    /**
     * GIF image content.
     */
    IMAGE_GIF("image/gif", "gif"),

    /**
     * WebP image content.
     */
    IMAGE_WEBP("image/webp", "webp"),

    /**
     * SVG image content.
     */
    IMAGE_SVG("image/svg+xml", "svg"),

    /**
     * Icon image content.
     */
    IMAGE_ICON("image/x-icon", "ico"),

    /**
     * HEIC image content.
     */
    IMAGE_HEIC("image/heic", "heic"),

    /**
     * HEIF image content.
     */
    IMAGE_HEIF("image/heif", "heif"),

    /**
     * MPEG audio content.
     */
    AUDIO_MPEG("audio/mpeg", "mp3"),

    /**
     * MP4 video content.
     */
    VIDEO_MP4("video/mp4", "mp4");

    /**
     * The textual representation of the content type.
     */
    private final String value;

    /**
     * The supported file extensions.
     */
    private final String[] extensions;

    /**
     * Creates a content type.
     *
     * @param value
     *     the textual representation of the content type.
     * @param extensions
     *     the supported file extensions.
     */
    ContentType(
        final String value,
        final String... extensions
    ) {
        this.value = value;
        this.extensions = extensions;
    }

    /**
     * Returns the textual representation of this content type.
     *
     * @return
     *     the textual representation.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the content type corresponding to the specified text.
     * <p>
     * Content type parameters, such as {@code charset} and {@code boundary},
     * are ignored during recognition.
     *
     * @param value
     *     the textual representation of the content type.
     * @return
     *     the corresponding content type, or
     *     {@link #APPLICATION_OCTET_STREAM} if the value is {@code null},
     *     empty, or not recognized.
     */
    public static ContentType fromString(final String value) {
        if (value == null) {
            return APPLICATION_OCTET_STREAM;
        }

        final int parameterIndex = value.indexOf(';');
        final String normalized = (
            parameterIndex >= 0
                ? value.substring(0, parameterIndex)
                : value
        ).trim().toLowerCase(Locale.ENGLISH);

        for (ContentType contentType : values()) {
            if (contentType.value.equals(normalized)) {
                return contentType;
            }
        }

        return APPLICATION_OCTET_STREAM;
    }

    /**
     * Returns the content type corresponding to the specified file extension.
     * <p>
     * The leading dot, if present, is ignored. Extension comparison is
     * case-insensitive.
     *
     * @param value
     *     the file extension.
     * @return
     *     the corresponding content type, or
     *     {@link #APPLICATION_OCTET_STREAM} if the extension is {@code null},
     *     empty, or not recognized.
     */
    public static ContentType fromExtension(final String value) {
        if (value == null) {
            return APPLICATION_OCTET_STREAM;
        }

        String normalized = value.trim();

        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }

        normalized = normalized.toLowerCase(Locale.ENGLISH);

        if (normalized.isEmpty()) {
            return APPLICATION_OCTET_STREAM;
        }

        for (ContentType contentType : values()) {
            for (String extension : contentType.extensions) {
                if (extension.equals(normalized)) {
                    return contentType;
                }
            }
        }

        return APPLICATION_OCTET_STREAM;
    }

    /**
     * Returns the textual representation of this content type.
     *
     * @return
     *     the textual representation.
     */
    @Override
    public String toString() {
        return value;
    }
}
