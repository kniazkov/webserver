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
    TEXT_PLAIN("text/plain"),

    /**
     * HTML content.
     */
    TEXT_HTML("text/html"),

    /**
     * CSS content.
     */
    TEXT_CSS("text/css"),

    /**
     * JavaScript content.
     */
    TEXT_JAVASCRIPT("text/javascript"),

    /**
     * JSON content.
     */
    APPLICATION_JSON("application/json"),

    /**
     * XML application content.
     */
    APPLICATION_XML("application/xml"),

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
    APPLICATION_OCTET_STREAM("application/octet-stream"),

    /**
     * Portable document format content.
     */
    APPLICATION_PDF("application/pdf"),

    /**
     * ZIP archive content.
     */
    APPLICATION_ZIP("application/zip"),

    /**
     * PNG image content.
     */
    IMAGE_PNG("image/png"),

    /**
     * JPEG image content.
     */
    IMAGE_JPEG("image/jpeg"),

    /**
     * GIF image content.
     */
    IMAGE_GIF("image/gif"),

    /**
     * WebP image content.
     */
    IMAGE_WEBP("image/webp"),

    /**
     * SVG image content.
     */
    IMAGE_SVG("image/svg+xml"),

    /**
     * Icon image content.
     */
    IMAGE_ICON("image/x-icon"),

    /**
     * HEIC image content.
     */
    IMAGE_HEIC("image/heic"),

    /**
     * HEIF image content.
     */
    IMAGE_HEIF("image/heif"),

    /**
     * MPEG audio content.
     */
    AUDIO_MPEG("audio/mpeg"),

    /**
     * MP4 video content.
     */
    VIDEO_MP4("video/mp4");

    /**
     * The textual representation of the content type.
     */
    private final String value;

    /**
     * Creates a content type.
     *
     * @param value
     *     the textual representation of the content type.
     */
    ContentType(final String value) {
        this.value = value;
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
