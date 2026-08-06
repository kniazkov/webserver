/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link Response.Builder}.
 */
final class DefaultResponseBuilder implements Response.Builder {

    /**
     * The HTTP response status.
     */
    private HttpStatus status = HttpStatus.OK;

    /**
     * The response content type.
     */
    private ContentType contentType = ContentType.APPLICATION_OCTET_STREAM;

    /**
     * The additional response header fields.
     */
    private final Map<String, List<String>> headers = new LinkedHashMap<>();

    /**
     * The response cookies.
     */
    private final Map<String, String> cookies = new LinkedHashMap<>();

    /**
     * The response body data.
     */
    private byte[] data = new byte[0];

    /**
     * Sets the HTTP response status.
     *
     * @param status
     *     the response status.
     * @return
     *     this builder.
     */
    @Override
    public Response.Builder setStatus(final HttpStatus status) {
        this.status = Objects.requireNonNull(
            status,
            "Response status must not be null."
        );
        return this;
    }

    /**
     * Sets the response content type.
     *
     * @param contentType
     *     the response content type.
     * @return
     *     this builder.
     */
    @Override
    public Response.Builder setContentType(
        final ContentType contentType
    ) {
        this.contentType = Objects.requireNonNull(
            contentType,
            "Content type must not be null."
        );
        return this;
    }

    /**
     * Sets the response body data.
     *
     * @param data
     *     the response body data.
     * @return
     *     this builder.
     */
    @Override
    public Response.Builder setData(final byte[] data) {
        this.data = Arrays.copyOf(
            Objects.requireNonNull(
                data,
                "Response data must not be null."
            ),
            data.length
        );
        return this;
    }

    /**
     * Sets the response body to the specified plain text.
     *
     * @param text
     *     the response text.
     * @return
     *     this builder.
     */
    @Override
    public Response.Builder setPlainText(final String text) {
        contentType = ContentType.TEXT_PLAIN;
        data = Objects.requireNonNull(
            text,
            "Response text must not be null."
        ).getBytes(StandardCharsets.UTF_8);
        return this;
    }

    /**
     * Sets the response body to the specified HTML document.
     *
     * @param html
     *     the HTML document.
     * @return
     *     this builder.
     */
    @Override
    public Response.Builder setHtml(final String html) {
        contentType = ContentType.TEXT_HTML;
        data = Objects.requireNonNull(
            html,
            "HTML document must not be null."
        ).getBytes(StandardCharsets.UTF_8);
        return this;
    }

    /**
     * Sets the response body to the specified JSON document.
     *
     * @param json
     *     the JSON document.
     * @return
     *     this builder.
     */
    @Override
    public Response.Builder setJson(final String json) {
        contentType = ContentType.APPLICATION_JSON;
        data = Objects.requireNonNull(
            json,
            "JSON document must not be null."
        ).getBytes(StandardCharsets.UTF_8);
        return this;
    }

    /**
     * Adds an HTTP response header field.
     *
     * @param name
     *     the header field name.
     * @param value
     *     the header field value.
     * @return
     *     this builder.
     */
    @Override
    public Response.Builder addHeader(
        final String name,
        final String value
    ) {
        if (HttpHeaders.isManaged(name)) {
            throw new IllegalArgumentException(
                "Header is managed by the server: " + name
            );
        }
        headers.computeIfAbsent(
            Objects.requireNonNull(
                name,
                "Header name must not be null."
            ),
            key -> new ArrayList<>()
        ).add(
            Objects.requireNonNull(
                value,
                "Header value must not be null."
            )
        );

        return this;
    }

    /**
     * Replaces all values of an HTTP response header field.
     *
     * @param name
     *     the header field name.
     * @param value
     *     the header field value.
     * @return
     *     this builder.
     */
    @Override
    public Response.Builder setHeader(
        final String name,
        final String value
    ) {
        if (HttpHeaders.isManaged(name)) {
            throw new IllegalArgumentException(
                "Header is managed by the server: " + name
            );
        }
        headers.put(
            Objects.requireNonNull(
                name,
                "Header name must not be null."
            ),
            new ArrayList<>(
                List.of(
                    Objects.requireNonNull(
                        value,
                        "Header value must not be null."
                    )
                )
            )
        );

        return this;
    }

    /**
     * Sets a response cookie.
     * <p>
     * If a cookie with the same name already exists, its value is replaced.
     *
     * @param name
     *     the cookie name.
     * @param value
     *     the cookie value.
     * @return
     *     this builder.
     */
    @Override
    public Response.Builder setCookie(
        final String name,
        final String value
    ) {
        cookies.put(
            Objects.requireNonNull(
                name,
                "Cookie name must not be null."
            ),
            Objects.requireNonNull(
                value,
                "Cookie value must not be null."
            )
        );
        return this;
    }

    /**
     * Builds an immutable HTTP response.
     *
     * @return
     *     the immutable response.
     */
    @Override
    public Response build() {
        final Map<String, List<String>> result =
            new LinkedHashMap<>(headers);

        if (!cookies.isEmpty()) {
            final List<String> values = result.computeIfAbsent(
                "Set-Cookie",
                key -> new ArrayList<>()
            );

            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                values.add(entry.getKey() + "=" + entry.getValue());
            }
        }

        return new DefaultResponse(
            status,
            contentType,
            result,
            data
        );
    }
}
