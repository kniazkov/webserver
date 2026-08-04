/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete HTTP request.
 * <p>
 * This class is immutable. All mutable data supplied through the builder is
 * copied when the request is created. The request body is also copied whenever
 * it is returned to the caller.
 */
public final class Request {

    /**
     * The parsed HTTP request header.
     */
    private final RequestHeader header;

    /**
     * The query parameters extracted from the request target.
     */
    private final Map<String, List<String>> query;

    /**
     * The form parameters extracted from the request body.
     */
    private final Map<String, List<String>> form;

    /**
     * The uploaded files grouped by form field name.
     */
    private final Map<String, List<UploadedFile>> files;

    /**
     * The cookies supplied with the request.
     */
    private final Map<String, String> cookies;

    /**
     * The original request body.
     */
    private final byte[] body;

    /**
     * Creates a request from the specified builder.
     *
     * @param builder
     *     the builder containing the request data.
     */
    private Request(final Builder builder) {
        this.header = builder.header;
        this.query = copyValues(builder.query);
        this.form = copyValues(builder.form);
        this.files = copyFiles(builder.files);
        this.cookies = Collections.unmodifiableMap(
            new LinkedHashMap<>(builder.cookies)
        );
        this.body = Arrays.copyOf(builder.body, builder.body.length);
    }

    /**
     * Returns the parsed HTTP request header.
     *
     * @return
     *     the request header.
     */
    public RequestHeader getHeader() {
        return header;
    }

    /**
     * Returns the query parameters extracted from the request target.
     *
     * @return
     *     an immutable map of query parameters.
     */
    public Map<String, List<String>> getQuery() {
        return query;
    }

    /**
     * Returns the form parameters extracted from the request body.
     *
     * @return
     *     an immutable map of form parameters.
     */
    public Map<String, List<String>> getForm() {
        return form;
    }

    /**
     * Returns the uploaded files grouped by form field name.
     *
     * @return
     *     an immutable map of uploaded files.
     */
    public Map<String, List<UploadedFile>> getFiles() {
        return files;
    }

    /**
     * Returns the cookies supplied with the request.
     *
     * @return
     *     an immutable map of cookies.
     */
    public Map<String, String> getCookies() {
        return cookies;
    }

    /**
     * Returns a copy of the original request body.
     *
     * @return
     *     a copy of the request body.
     */
    public byte[] getBody() {
        return Arrays.copyOf(body, body.length);
    }

    /**
     * Returns the size of the request body in bytes.
     *
     * @return
     *     the body size in bytes.
     */
    public int getBodySize() {
        return body.length;
    }

    /**
     * Creates a new request builder.
     *
     * @return
     *     the new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an immutable copy of a map containing lists of string values.
     *
     * @param source
     *     the source map.
     * @return
     *     the immutable copy.
     */
    private static Map<String, List<String>> copyValues(
        final Map<String, List<String>> source
    ) {
        final Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Creates an immutable copy of a map containing uploaded files.
     *
     * @param source
     *     the source map.
     * @return
     *     the immutable copy.
     */
    private static Map<String, List<UploadedFile>> copyFiles(
        final Map<String, List<UploadedFile>> source
    ) {
        final Map<String, List<UploadedFile>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<UploadedFile>> entry : source.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Builds immutable HTTP requests.
     */
    public static final class Builder {

        /**
         * The parsed HTTP request header.
         */
        private RequestHeader header;

        /**
         * The query parameters.
         */
        private final Map<String, List<String>> query = new LinkedHashMap<>();

        /**
         * The form parameters.
         */
        private final Map<String, List<String>> form = new LinkedHashMap<>();

        /**
         * The uploaded files grouped by form field name.
         */
        private final Map<String, List<UploadedFile>> files = new LinkedHashMap<>();

        /**
         * The request cookies.
         */
        private final Map<String, String> cookies = new LinkedHashMap<>();

        /**
         * The original request body.
         */
        private byte[] body = new byte[0];

        /**
         * Sets the parsed HTTP request header.
         *
         * @param header
         *     the request header.
         * @return
         *     this builder.
         */
        public Builder setHeader(final RequestHeader header) {
            this.header = header;
            return this;
        }

        /**
         * Adds a query parameter value.
         *
         * @param name
         *     the parameter name.
         * @param value
         *     the parameter value.
         * @return
         *     this builder.
         */
        public Builder addQueryValue(
            final String name,
            final String value
        ) {
            query.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
            return this;
        }

        /**
         * Adds a form parameter value.
         *
         * @param name
         *     the parameter name.
         * @param value
         *     the parameter value.
         * @return
         *     this builder.
         */
        public Builder addFormValue(
            final String name,
            final String value
        ) {
            form.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
            return this;
        }

        /**
         * Adds an uploaded file.
         *
         * @param fieldName
         *     the name of the form field containing the file.
         * @param file
         *     the uploaded file.
         * @return
         *     this builder.
         */
        public Builder addFile(
            final String fieldName,
            final UploadedFile file
        ) {
            files.computeIfAbsent(fieldName, key -> new ArrayList<>()).add(file);
            return this;
        }

        /**
         * Sets a cookie value.
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
        public Builder setCookie(
            final String name,
            final String value
        ) {
            cookies.put(name, value);
            return this;
        }

        /**
         * Sets the original request body.
         *
         * @param body
         *     the request body.
         * @return
         *     this builder.
         * @throws IllegalArgumentException
         *     if the body is {@code null}.
         */
        public Builder setBody(final byte[] body) {
            if (body == null) {
                throw new IllegalArgumentException(
                    "Request body must not be null."
                );
            }
            this.body = Arrays.copyOf(body, body.length);
            return this;
        }

        /**
         * Builds an immutable HTTP request.
         *
         * @return
         *     the HTTP request.
         * @throws ServerException
         *     if the request header is not specified.
         */
        public Request build() throws ServerException {
            if (header == null) {
                throw new ServerException(
                    "Request header is not specified."
                );
            }
            return new Request(this);
        }
    }
}
