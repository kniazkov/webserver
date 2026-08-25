/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Options;

import com.microsoft.playwright.options.RequestOptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for multipart metadata limits.
 */
final class MultipartLimitsEndToEndTest extends EndToEndBaseTest {

    /**
     * Test multipart boundary.
     */
    private static final String BOUNDARY = "e2e-boundary";

    /**
     * Configures restrictive multipart metadata limits.
     *
     * @param builder
     *     the server options builder.
     */
    @Override
    protected void configure(final Options.Builder builder) {
        builder
            .setMaxMultipartParts(1)
            .setMaxMultipartHeaderSize(64);
        super.configure(builder);
    }

    /**
     * Tests that an excessive part count produces HTTP 413.
     */
    @Test
    void tooManyParts() throws Exception {
        startServer();

        final String body =
            part(BOUNDARY, "first", "one", false)
                + part(BOUNDARY, "second", "two", true);

        assertEquals(413, post(BOUNDARY, body));
    }

    /**
     * Tests that excessive part headers produce HTTP 413.
     */
    @Test
    void partHeadersTooLarge() throws Exception {
        startServer();

        final String body =
            "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"field\"\r\n"
                + "X-Padding: " + "x".repeat(64) + "\r\n"
                + "\r\n"
                + "value\r\n"
                + "--" + BOUNDARY + "--";

        assertEquals(413, post(BOUNDARY, body));
    }

    /**
     * Tests that a boundary longer than RFC 2046 permits produces HTTP 400.
     */
    @Test
    void boundaryTooLong() throws Exception {
        startServer();

        final String boundary = "b".repeat(71);
        final String body = part(boundary, "field", "value", true);

        assertEquals(400, post(boundary, body));
    }

    /**
     * Sends a multipart request and returns its response status.
     *
     * @param boundary
     *     the multipart boundary.
     * @param body
     *     the request body.
     * @return
     *     the HTTP response status.
     */
    private int post(final String boundary, final String body) {
        return page.request().post(
            url("/upload"),
            RequestOptions.create()
                .setHeader(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
                )
                .setData(body)
        ).status();
    }

    /**
     * Creates one multipart form field including its preceding boundary.
     *
     * @param boundary
     *     the multipart boundary.
     * @param name
     *     the form field name.
     * @param value
     *     the form field value.
     * @param last
     *     whether this is the last field.
     * @return
     *     the encoded multipart part.
     */
    private static String part(
        final String boundary,
        final String name,
        final String value,
        final boolean last
    ) {
        return "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\""
            + name + "\"\r\n"
            + "\r\n"
            + value + "\r\n"
            + (last ? "--" + boundary + "--" : "");
    }
}
