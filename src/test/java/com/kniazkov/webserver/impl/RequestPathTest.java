/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.RequestPath;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests request path parsing.
 */
final class RequestPathTest {

    /**
     * Tests the root path.
     */
    @Test
    void root() throws ServerException {
        final RequestPath path = RequestPathImpl.build("/");

        assertSame(
            RootRequestPath.getInstance(),
            path
        );

        assertEquals("/", path.getPath());
        assertEquals("/", path.getDirectory());
        assertEquals("", path.getFileName());
        assertEquals("", path.getFileType());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            path.getContentType()
        );
    }

    /**
     * Tests a file in the root directory.
     */
    @Test
    void rootFile() throws ServerException {
        final RequestPath path =
            RequestPathImpl.build("/index.html");

        assertEquals("/index.html", path.getPath());
        assertEquals("/", path.getDirectory());
        assertEquals("index.html", path.getFileName());
        assertEquals("html", path.getFileType());
        assertEquals(
            ContentType.TEXT_HTML,
            path.getContentType()
        );
    }

    /**
     * Tests a file in a nested directory.
     */
    @Test
    void nestedFile() throws ServerException {
        final RequestPath path =
            RequestPathImpl.build("/images/icons/logo.png");

        assertEquals(
            "/images/icons/logo.png",
            path.getPath()
        );
        assertEquals(
            "/images/icons/",
            path.getDirectory()
        );
        assertEquals("logo.png", path.getFileName());
        assertEquals("png", path.getFileType());
        assertEquals(
            ContentType.IMAGE_PNG,
            path.getContentType()
        );
    }

    /**
     * Tests a file without an extension.
     */
    @Test
    void fileWithoutExtension() throws ServerException {
        final RequestPath path =
            RequestPathImpl.build("/download");

        assertEquals("/download", path.getPath());
        assertEquals("/", path.getDirectory());
        assertEquals("download", path.getFileName());
        assertEquals("", path.getFileType());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            path.getContentType()
        );
    }

    /**
     * Tests a file with several dots in its name.
     */
    @Test
    void severalDots() throws ServerException {
        final RequestPath path =
            RequestPathImpl.build("/archive/data.backup.zip");

        assertEquals(
            "/archive/data.backup.zip",
            path.getPath()
        );
        assertEquals(
            "/archive/",
            path.getDirectory()
        );
        assertEquals(
            "data.backup.zip",
            path.getFileName()
        );
        assertEquals("zip", path.getFileType());
        assertEquals(
            ContentType.APPLICATION_ZIP,
            path.getContentType()
        );
    }

    /**
     * Tests an unknown file extension.
     */
    @Test
    void unknownExtension() throws ServerException {
        final RequestPath path =
            RequestPathImpl.build("/file.whatever");

        assertEquals("/file.whatever", path.getPath());
        assertEquals("/", path.getDirectory());
        assertEquals("file.whatever", path.getFileName());
        assertEquals("whatever", path.getFileType());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            path.getContentType()
        );
    }

    /**
     * Tests an upper-case file extension.
     */
    @Test
    void upperCaseExtension() throws ServerException {
        final RequestPath path =
            RequestPathImpl.build("/IMAGE.PNG");

        assertEquals("/IMAGE.PNG", path.getPath());
        assertEquals("/", path.getDirectory());
        assertEquals("IMAGE.PNG", path.getFileName());
        assertEquals("png", path.getFileType());
        assertEquals(
            ContentType.IMAGE_PNG,
            path.getContentType()
        );
    }

    /**
     * Tests mixed-case file extensions.
     */
    @Test
    void mixedCaseExtensions() throws ServerException {
        final RequestPath jpeg =
            RequestPathImpl.build("/photo.JpEg");

        final RequestPath html =
            RequestPathImpl.build("/INDEX.HtMl");

        final RequestPath pdf =
            RequestPathImpl.build("/docs/report.PdF");

        assertEquals("jpeg", jpeg.getFileType());
        assertEquals(
            ContentType.IMAGE_JPEG,
            jpeg.getContentType()
        );

        assertEquals("html", html.getFileType());
        assertEquals(
            ContentType.TEXT_HTML,
            html.getContentType()
        );

        assertEquals("pdf", pdf.getFileType());
        assertEquals(
            ContentType.APPLICATION_PDF,
            pdf.getContentType()
        );
    }

    /**
     * Tests a hidden file without an extension.
     */
    @Test
    void hiddenFile() throws ServerException {
        final RequestPath path =
            RequestPathImpl.build("/.gitignore");

        assertEquals("/.gitignore", path.getPath());
        assertEquals("/", path.getDirectory());
        assertEquals(".gitignore", path.getFileName());
        assertEquals("", path.getFileType());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            path.getContentType()
        );
    }

    /**
     * Tests a file name ending with a dot.
     */
    @Test
    void trailingDot() throws ServerException {
        final RequestPath path =
            RequestPathImpl.build("/file.");

        assertEquals("/file.", path.getPath());
        assertEquals("/", path.getDirectory());
        assertEquals("file.", path.getFileName());
        assertEquals("", path.getFileType());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            path.getContentType()
        );
    }

    /**
     * Tests percent decoding with strict UTF-8.
     */
    @Test
    void percentEncodedPath() throws ServerException {
        final RequestPath path = RequestPathImpl.build(
            "/documents/My%20caf%C3%A9.%74xt"
        );

        assertEquals(
            "/documents/My café.txt",
            path.getPath()
        );
        assertEquals("/documents/", path.getDirectory());
        assertEquals("My café.txt", path.getFileName());
        assertEquals("txt", path.getFileType());
        assertEquals(ContentType.TEXT_PLAIN, path.getContentType());
    }

    /**
     * Tests that path decoding is distinct from form decoding.
     */
    @Test
    void pathEncodingRules() throws ServerException {
        final RequestPath path = RequestPathImpl.build(
            "/a+b%3F%23%252e.txt"
        );

        assertEquals("/a+b?#%2e.txt", path.getPath());
        assertEquals("a+b?#%2e.txt", path.getFileName());
    }

    /**
     * Tests rejection of malformed and non-UTF-8 percent encoding.
     */
    @Test
    void invalidPercentEncoding() {
        final List<String> invalid = List.of(
            "/file%",
            "/file%2",
            "/file%GG",
            "/file%C3%28",
            "/file%C0%AF"
        );

        for (String path : invalid) {
            assertThrows(
                ServerException.class,
                () -> RequestPathImpl.build(path),
                path
            );
        }
    }

    /**
     * Tests rejection of literal and percent-encoded traversal segments.
     */
    @Test
    void traversalSegments() {
        final List<String> invalid = List.of(
            "/../secret.txt",
            "/%2e%2e/secret.txt",
            "/.%2e/secret.txt",
            "/%2E./secret.txt",
            "/safe/%2e/secret.txt",
            "/safe/%2e%2e/secret.txt"
        );

        for (String path : invalid) {
            assertThrows(
                ServerException.class,
                () -> RequestPathImpl.build(path),
                path
            );
        }
    }

    /**
     * Tests rejection of literal and percent-encoded path separators inside a
     * segment.
     */
    @Test
    void encodedSeparators() {
        final List<String> invalid = List.of(
            "/safe\\secret.txt",
            "/safe%5csecret.txt",
            "/safe%2Fsecret.txt"
        );

        for (String path : invalid) {
            assertThrows(
                ServerException.class,
                () -> RequestPathImpl.build(path),
                path
            );
        }
    }

    /**
     * Tests rejection of control characters and non-ASCII literal data.
     */
    @Test
    void invalidDecodedCharacters() {
        final List<String> invalid = List.of(
            "/file%00.txt",
            "/file%0A.txt",
            "/café.txt"
        );

        for (String path : invalid) {
            assertThrows(
                ServerException.class,
                () -> RequestPathImpl.build(path),
                path
            );
        }
    }

    /**
     * Tests a null path.
     */
    @Test
    void nullPath() {
        assertThrows(
            ServerException.class,
            () -> RequestPathImpl.build(null)
        );
    }

    /**
     * Tests an empty path.
     */
    @Test
    void emptyPath() {
        assertThrows(
            ServerException.class,
            () -> RequestPathImpl.build("")
        );
    }

    /**
     * Tests a path without a leading slash.
     */
    @Test
    void missingLeadingSlash() {
        assertThrows(
            ServerException.class,
            () -> RequestPathImpl.build("index.html")
        );
    }

    /**
     * Tests a path containing a query string.
     */
    @Test
    void queryInsidePath() {
        assertThrows(
            ServerException.class,
            () -> RequestPathImpl.build(
                "/index.html?q=test"
            )
        );
    }

    /**
     * Tests a path containing a fragment.
     */
    @Test
    void fragmentInsidePath() {
        assertThrows(
            ServerException.class,
            () -> RequestPathImpl.build(
                "/index.html#section"
            )
        );
    }

    /**
     * Tests a path containing a backslash.
     */
    @Test
    void backslash() {
        assertThrows(
            ServerException.class,
            () -> RequestPathImpl.build(
                "/images\\logo.png"
            )
        );
    }

    /**
     * Tests an empty path segment.
     */
    @Test
    void emptySegment() {
        assertThrows(
            ServerException.class,
            () -> RequestPathImpl.build(
                "/images//logo.png"
            )
        );
    }

    /**
     * Tests the current-directory path segment.
     */
    @Test
    void currentDirectorySegment() {
        assertThrows(
            ServerException.class,
            () -> RequestPathImpl.build(
                "/images/./logo.png"
            )
        );
    }

    /**
     * Tests the parent-directory path segment.
     */
    @Test
    void parentDirectorySegment() {
        assertThrows(
            ServerException.class,
            () -> RequestPathImpl.build(
                "/images/../logo.png"
            )
        );
    }

    /**
     * Tests a path ending with a slash.
     */
    @Test
    void directoryWithoutFile() throws ServerException {
        final RequestPath path = RequestPathImpl.build(
            "/images/"
        );

        assertEquals("/images/", path.getPath());
        assertEquals("/images/", path.getDirectory());
        assertEquals("", path.getFileName());
        assertEquals("", path.getFileType());
        assertEquals(
            ContentType.APPLICATION_OCTET_STREAM,
            path.getContentType()
        );
    }
}
