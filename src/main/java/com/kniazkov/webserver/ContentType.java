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
     * Comma-separated values content.
     */
    TEXT_CSV("text/csv", "csv"),

    /**
     * Markdown content.
     */
    TEXT_MARKDOWN("text/markdown", "md", "markdown"),

    /**
     * Calendar content.
     */
    TEXT_CALENDAR("text/calendar", "ics", "ifb"),

    /**
     * Contact card content.
     */
    TEXT_VCARD("text/vcard", "vcf", "vcard"),

    /**
     * Server-sent event stream content.
     */
    TEXT_EVENT_STREAM("text/event-stream"),

    /**
     * JSON content.
     */
    APPLICATION_JSON("application/json", "json"),

    /**
     * JSON-LD content.
     */
    APPLICATION_LD_JSON("application/ld+json", "jsonld"),

    /**
     * Problem details represented as JSON.
     */
    APPLICATION_PROBLEM_JSON("application/problem+json"),

    /**
     * Web application manifest content.
     */
    APPLICATION_MANIFEST_JSON(
        "application/manifest+json",
        "webmanifest"
    ),

    /**
     * XML application content.
     */
    APPLICATION_XML("application/xml", "xml"),

    /**
     * XHTML content.
     */
    APPLICATION_XHTML_XML("application/xhtml+xml", "xhtml", "xht"),

    /**
     * RSS feed content.
     */
    APPLICATION_RSS_XML("application/rss+xml", "rss"),

    /**
     * Atom feed content.
     */
    APPLICATION_ATOM_XML("application/atom+xml", "atom"),

    /**
     * YAML content.
     */
    APPLICATION_YAML("application/yaml", "yaml", "yml"),

    /**
     * TOML content.
     */
    APPLICATION_TOML("application/toml", "toml"),

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
     * Gzip-compressed content.
     */
    APPLICATION_GZIP("application/gzip", "gz"),

    /**
     * Zstandard-compressed content.
     */
    APPLICATION_ZSTD("application/zstd", "zst"),

    /**
     * Tar archive content.
     */
    APPLICATION_X_TAR("application/x-tar", "tar"),

    /**
     * Bzip2-compressed content.
     */
    APPLICATION_X_BZIP2("application/x-bzip2", "bz2", "boz"),

    /**
     * 7-Zip archive content.
     */
    APPLICATION_X_7Z_COMPRESSED(
        "application/x-7z-compressed",
        "7z"
    ),

    /**
     * RAR archive content.
     */
    APPLICATION_VND_RAR("application/vnd.rar", "rar"),

    /**
     * WebAssembly module content.
     */
    APPLICATION_WASM("application/wasm", "wasm"),

    /**
     * Java archive content.
     */
    APPLICATION_JAVA_ARCHIVE(
        "application/java-archive",
        "jar",
        "war",
        "ear"
    ),

    /**
     * Rich Text Format content.
     */
    APPLICATION_RTF("application/rtf", "rtf"),

    /**
     * EPUB publication content.
     */
    APPLICATION_EPUB_ZIP("application/epub+zip", "epub"),

    /**
     * PostScript content.
     */
    APPLICATION_POSTSCRIPT(
        "application/postscript",
        "ai",
        "eps",
        "ps"
    ),

    /**
     * SQL content.
     */
    APPLICATION_SQL("application/sql", "sql"),

    /**
     * Concise Binary Object Representation content.
     */
    APPLICATION_CBOR("application/cbor", "cbor"),

    /**
     * GeoJSON content.
     */
    APPLICATION_GEO_JSON("application/geo+json", "geojson"),

    /**
     * Microsoft Word document content.
     */
    APPLICATION_MSWORD("application/msword", "doc", "dot"),

    /**
     * Office Open XML word-processing document content.
     */
    APPLICATION_OPENXML_WORD_DOCUMENT(
        "application/vnd.openxmlformats-officedocument."
            + "wordprocessingml.document",
        "docx"
    ),

    /**
     * Microsoft Excel spreadsheet content.
     */
    APPLICATION_VND_MS_EXCEL(
        "application/vnd.ms-excel",
        "xls",
        "xlt",
        "xla"
    ),

    /**
     * Office Open XML spreadsheet content.
     */
    APPLICATION_OPENXML_SPREADSHEET(
        "application/vnd.openxmlformats-officedocument."
            + "spreadsheetml.sheet",
        "xlsx"
    ),

    /**
     * Microsoft PowerPoint presentation content.
     */
    APPLICATION_VND_MS_POWERPOINT(
        "application/vnd.ms-powerpoint",
        "ppt",
        "pps",
        "pot"
    ),

    /**
     * Office Open XML presentation content.
     */
    APPLICATION_OPENXML_PRESENTATION(
        "application/vnd.openxmlformats-officedocument."
            + "presentationml.presentation",
        "pptx"
    ),

    /**
     * OpenDocument text content.
     */
    APPLICATION_OPENDOCUMENT_TEXT(
        "application/vnd.oasis.opendocument.text",
        "odt"
    ),

    /**
     * OpenDocument spreadsheet content.
     */
    APPLICATION_OPENDOCUMENT_SPREADSHEET(
        "application/vnd.oasis.opendocument.spreadsheet",
        "ods"
    ),

    /**
     * OpenDocument presentation content.
     */
    APPLICATION_OPENDOCUMENT_PRESENTATION(
        "application/vnd.oasis.opendocument.presentation",
        "odp"
    ),

    /**
     * SQLite database content.
     */
    APPLICATION_VND_SQLITE3(
        "application/vnd.sqlite3",
        "sqlite",
        "sqlite3"
    ),

    /**
     * Google Earth KML content.
     */
    APPLICATION_VND_GOOGLE_EARTH_KML_XML(
        "application/vnd.google-earth.kml+xml",
        "kml"
    ),

    /**
     * Google Earth KMZ content.
     */
    APPLICATION_VND_GOOGLE_EARTH_KMZ(
        "application/vnd.google-earth.kmz",
        "kmz"
    ),

    /**
     * Apple installer package content.
     */
    APPLICATION_VND_APPLE_INSTALLER_XML(
        "application/vnd.apple.installer+xml",
        "mpkg"
    ),

    /**
     * PKCS #12 archive content.
     */
    APPLICATION_PKCS12("application/pkcs12", "p12", "pfx"),

    /**
     * RFC 822 message content.
     */
    MESSAGE_RFC822("message/rfc822", "eml", "mime"),

    /**
     * PNG image content.
     */
    IMAGE_PNG("image/png", "png"),

    /**
     * Animated PNG image content.
     */
    IMAGE_APNG("image/apng", "apng"),

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
     * AV1 image content.
     */
    IMAGE_AVIF("image/avif", "avif"),

    /**
     * JPEG XL image content.
     */
    IMAGE_JXL("image/jxl", "jxl"),

    /**
     * Bitmap image content.
     */
    IMAGE_BMP("image/bmp", "bmp", "dib"),

    /**
     * TIFF image content.
     */
    IMAGE_TIFF("image/tiff", "tif", "tiff"),

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
     * Adobe Photoshop image content.
     */
    IMAGE_VND_ADOBE_PHOTOSHOP(
        "image/vnd.adobe.photoshop",
        "psd"
    ),

    /**
     * TrueType font content.
     */
    FONT_TTF("font/ttf", "ttf"),

    /**
     * OpenType font content.
     */
    FONT_OTF("font/otf", "otf"),

    /**
     * Web Open Font Format content.
     */
    FONT_WOFF("font/woff", "woff"),

    /**
     * Web Open Font Format 2 content.
     */
    FONT_WOFF2("font/woff2", "woff2"),

    /**
     * Font collection content.
     */
    FONT_COLLECTION("font/collection", "ttc"),

    /**
     * Embedded OpenType font content.
     */
    APPLICATION_VND_MS_FONTOBJECT(
        "application/vnd.ms-fontobject",
        "eot"
    ),

    /**
     * MPEG audio content.
     */
    AUDIO_MPEG("audio/mpeg", "mp3", "mp2", "mpga"),

    /**
     * AAC audio content.
     */
    AUDIO_AAC("audio/aac", "aac"),

    /**
     * FLAC audio content.
     */
    AUDIO_FLAC("audio/flac", "flac"),

    /**
     * MIDI audio content.
     */
    AUDIO_MIDI("audio/midi", "mid", "midi", "kar"),

    /**
     * MP4 audio content.
     */
    AUDIO_MP4("audio/mp4", "m4a", "m4b", "m4p"),

    /**
     * Ogg audio content.
     */
    AUDIO_OGG("audio/ogg", "oga", "ogg", "spx"),

    /**
     * Opus audio content.
     */
    AUDIO_OPUS("audio/opus", "opus"),

    /**
     * Waveform audio content.
     */
    AUDIO_WAV("audio/wav", "wav"),

    /**
     * WebM audio content.
     */
    AUDIO_WEBM("audio/webm", "weba"),

    /**
     * MP4 video content.
     */
    VIDEO_MP4("video/mp4", "mp4", "m4v"),

    /**
     * MPEG video content.
     */
    VIDEO_MPEG("video/mpeg", "mpeg", "mpg", "mpe", "m1v", "m2v"),

    /**
     * Ogg video content.
     */
    VIDEO_OGG("video/ogg", "ogv"),

    /**
     * QuickTime video content.
     */
    VIDEO_QUICKTIME("video/quicktime", "mov", "qt"),

    /**
     * WebM video content.
     */
    VIDEO_WEBM("video/webm", "webm"),

    /**
     * 3GPP video content.
     */
    VIDEO_3GPP("video/3gpp", "3gp", "3gpp"),

    /**
     * 3GPP2 video content.
     */
    VIDEO_3GPP2("video/3gpp2", "3g2", "3gpp2"),

    /**
     * AVI video content.
     */
    VIDEO_X_MSVIDEO("video/x-msvideo", "avi"),

    /**
     * Matroska video content.
     */
    VIDEO_X_MATROSKA("video/x-matroska", "mkv"),

    /**
     * glTF JSON model content.
     */
    MODEL_GLTF_JSON("model/gltf+json", "gltf"),

    /**
     * glTF binary model content.
     */
    MODEL_GLTF_BINARY("model/gltf-binary", "glb"),

    /**
     * Wavefront OBJ model content.
     */
    MODEL_OBJ("model/obj", "obj"),

    /**
     * Stereolithography model content.
     */
    MODEL_STL("model/stl", "stl"),

    /**
     * 3D Manufacturing Format content.
     */
    MODEL_3MF("model/3mf", "3mf"),

    /**
     * VRML model content.
     */
    MODEL_VRML("model/vrml", "vrml", "wrl");

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
