/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.List;
import java.util.Map;

/**
 * Represents an HTTP response produced by an application.
 * <p>
 * The response contains the status, content type, additional header fields,
 * and response body. A separate serializer is responsible for converting this
 * object into a valid HTTP message.
 */
public interface Response {

    /**
     * Creates a new response builder.
     *
     * @return
     *     the response builder.
     */
    static Builder builder() {
        return new DefaultResponseBuilder();
    }

    /**
     * Returns a shared HTTP 404 Not Found response.
     *
     * @return
     *     the shared response instance.
     */
    static Response notFound() {
        return NotFound.getInstance();
    }

    /**
     * Returns a shared HTTP 500 Internal Server Error response.
     *
     * @return
     *     the shared response instance.
     */
    static Response internalServerError() {
        return InternalServerError.getInstance();
    }

    /**
     * Returns the HTTP response status.
     *
     * @return
     *     the response status.
     */
    HttpStatus getStatus();

    /**
     * Returns the content type of the response body.
     *
     * @return
     *     the response content type.
     */
    ContentType getContentType();

    /**
     * Returns the additional HTTP response header fields.
     * <p>
     * The returned map must be immutable.
     *
     * @return
     *     the response header fields.
     */
    Map<String, List<String>> getHeaders();

    /**
     * Returns the response body data.
     *
     * @return
     *     a copy of the response body data.
     */
    byte[] getData();

    /**
     * Builds immutable HTTP responses.
     */
    interface Builder {

        /**
         * Sets the HTTP response status.
         *
         * @param status
         *     the response status.
         * @return
         *     this builder.
         */
        Builder setStatus(HttpStatus status);

        /**
         * Sets the response content type.
         *
         * @param contentType
         *     the response content type.
         * @return
         *     this builder.
         */
        Builder setContentType(ContentType contentType);

        /**
         * Sets the response body data.
         *
         * @param data
         *     the response body data.
         * @return
         *     this builder.
         */
        Builder setData(byte[] data);

        /**
         * Sets the response body to the specified plain text.
         * <p>
         * The content type is automatically set to
         * {@link ContentType#TEXT_PLAIN}.
         *
         * @param text
         *     the response text.
         * @return
         *     this builder.
         */
        Builder setPlainText(String text);

        /**
         * Sets the response body to the specified HTML document.
         * <p>
         * The content type is automatically set to
         * {@link ContentType#TEXT_HTML}.
         *
         * @param html
         *     the HTML document.
         * @return
         *     this builder.
         */
        Builder setHtml(String html);

        /**
         * Sets the response body to the specified JSON document.
         * <p>
         * The content type is automatically set to
         * {@link ContentType#APPLICATION_JSON}.
         *
         * @param json
         *     the JSON document.
         * @return
         *     this builder.
         */
        Builder setJson(String json);

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
        Builder addHeader(String name, String value);

        /**
         * Replaces all values of an HTTP response header field.
         *
         * @param name
         *     the header field name.
         * @param value
         *     the new header field value.
         * @return
         *     this builder.
         */
        Builder setHeader(String name, String value);

        /**
         * Adds a cookie to the response.
         *
         * @param name
         *     the cookie name.
         * @param value
         *     the cookie value.
         * @return
         *     this builder.
         */
        Builder setCookie(String name, String value);

        /**
         * Builds the HTTP response.
         *
         * @return
         *     the immutable response.
         */
        Response build();
    }
}
