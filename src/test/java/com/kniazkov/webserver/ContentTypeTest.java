/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests recognition of supported content types.
 */
final class ContentTypeTest {

    /**
     * Tests recognition of registered media type strings.
     */
    @Test
    void fromString() {
        assertEquals(
            ContentType.APPLICATION_PDF,
            ContentType.fromString(
                "Application/PDF; version=1.7"
            )
        );
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            ContentType.fromString("aplication/pdf")
        );
    }

    /**
     * Tests representative file types from the expanded registry.
     */
    @Test
    void fromExtension() {
        assertEquals(
            ContentType.FONT_WOFF2,
            ContentType.fromExtension(".WOFF2")
        );
        assertEquals(
            ContentType.APPLICATION_OPENXML_WORD_DOCUMENT,
            ContentType.fromExtension("docx")
        );
        assertEquals(
            ContentType.IMAGE_AVIF,
            ContentType.fromExtension("avif")
        );
        assertEquals(
            ContentType.MODEL_GLTF_JSON,
            ContentType.fromExtension("gltf")
        );
        assertEquals(
            ContentType.VIDEO_X_MATROSKA,
            ContentType.fromExtension("mkv")
        );
    }

    /**
     * Tests that every enum value represents a distinct media type.
     */
    @Test
    void uniqueValues() {
        final Set<String> unique = Arrays
            .stream(ContentType.values())
            .map(ContentType::getValue)
            .collect(Collectors.toSet());

        assertEquals(ContentType.values().length, unique.size());
    }
}
