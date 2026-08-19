/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.RequestPath;
import com.kniazkov.webserver.ServerException;

import java.util.List;

/**
 * Parses complete HTTP requests.
 */
final class RequestParser {

    /**
     * The source limited by the maximum request size.
     */
    private final RequestByteSource source;

    /**
     * The server options.
     */
    private final Options options;

    /**
     * The request builder.
     */
    private final RequestBuilder builder;

    /**
     * The parsed request headers.
     */
    private RequestHeaders headers;

    /**
     * Creates a request parser.
     *
     * @param source  the byte source.
     * @param options the server options.
     */
    private RequestParser(
        final ByteSource source,
        final Options options
    ) {
        this.options = options;
        this.source = new RequestByteSource(
            source,
            options.getMaxRequestSize()
        );
        builder = new RequestBuilder();
    }

    /**
     * Parses an HTTP request.
     *
     * @param source  the byte source.
     * @param options the server options.
     * @return the parsed request.
     * @throws ServerException if the request is invalid or cannot be read.
     */
    static Request parse(
        final ByteSource source,
        final Options options
    ) throws ServerException {
        return new RequestParser(source, options).parse();
    }

    /**
     * Parses the complete request.
     *
     * @return the parsed request.
     * @throws ServerException if the request is invalid.
     */
    private Request parse() throws ServerException {
        parseHeaders();
        validateVersion();
        parseTarget();
        parseCookies();
        parseBody();

        return builder.build();
    }

    /**
     * Parses the request headers.
     *
     * @throws ServerException if the headers are invalid.
     */
    private void parseHeaders() throws ServerException {
        final StringSource stringSource = new StringSource(
            source,
            options
        );

        headers = RequestHeadersParser.parse(stringSource);
        builder.setHeaders(headers);
    }

    /**
     * Validates HTTP-version-specific requirements.
     *
     * @throws ServerException if the request does not conform to its HTTP version.
     */
    private void validateVersion() throws ServerException {
        if (
            headers.getVersion() == HttpVersion.HTTP_1_1
                && !hasSingleNonEmptyHeader("Host")
        ) {
            throw new ServerException(
                "HTTP/1.1 request must contain exactly one Host header"
            );
        }

        if (headers.getValues().containsKey("Transfer-Encoding")) {
            throw new ServerException(
                "Transfer-Encoding is not supported"
            );
        }
    }

    /**
     * Parses the request target into path and query parameters.
     *
     * @throws ServerException if the request target is invalid.
     */
    private void parseTarget() throws ServerException {
        final String target = headers.getTarget();
        final int question = target.indexOf('?');

        final String path = question < 0
            ? target
            : target.substring(0, question);

        final RequestPath requestPath =
            RequestPathImpl.build(path);

        builder.setPath(requestPath);

        UrlEncodedParser.parseQuery(
            target,
            builder
        );
    }

    /**
     * Parses request cookies.
     *
     * @throws ServerException if the cookie header is invalid.
     */
    private void parseCookies() throws ServerException {
        CookieParser.parse(headers, builder);
    }

    /**
     * Reads and parses the request body.
     *
     * @throws ServerException if the body is invalid or incomplete.
     */
    private void parseBody() throws ServerException {
        final long contentLength = getContentLength();

        if (contentLength == 0) {
            return;
        }

        if (contentLength > Integer.MAX_VALUE) {
            throw new ServerException(
                "Request body is too large"
            );
        }

        final BodyByteSource bodySource = new BodyByteSource(
            source,
            contentLength
        );

        final ContentType contentType = getContentType();

        if (
            headers.getMethod() == HttpMethod.POST
                && contentType == ContentType.MULTIPART_FORM_DATA
        ) {
            final String boundary = getBoundary();

            MultipartParser.parse(
                bodySource,
                boundary,
                options,
                builder
            );

            /*
             * MultipartParser deliberately stops immediately after the final
             * boundary. Consume the rest of this HTTP body according to
             * Content-Length, but never bytes belonging to the next request.
             */
            bodySource.drain();
        } else {
            bodySource.drain();
        }

        final byte[] body = bodySource.getData();
        builder.setBody(body);

        if (
            headers.getMethod() == HttpMethod.POST
                && contentType
                == ContentType.APPLICATION_FORM_URLENCODED
        ) {
            UrlEncodedParser.parseForm(
                body,
                builder
            );
        }
    }

    /**
     * Returns the request content type.
     *
     * @return the content type.
     * @throws ServerException if several Content-Type headers are present.
     */
    private ContentType getContentType() throws ServerException {
        final List<String> values =
            headers.getValues().get("Content-Type");

        if (values == null) {
            return ContentType.APPLICATION_OCTET_STREAM;
        }

        if (values.size() != 1) {
            throw new ServerException(
                "Multiple Content-Type headers are not allowed"
            );
        }

        return ContentType.fromString(values.getFirst());
    }

    /**
     * Returns the declared request body length.
     *
     * @return the body length.
     * @throws ServerException if Content-Length is invalid.
     */
    private long getContentLength() throws ServerException {
        final List<String> values =
            headers.getValues().get("Content-Length");

        if (values == null) {
            return 0;
        }

        if (values.size() != 1) {
            throw new ServerException(
                "Multiple Content-Length headers are not allowed"
            );
        }

        final String value = values.getFirst();

        try {
            final long length = Long.parseLong(value);

            if (length < 0) {
                throw new ServerException(
                    "Invalid Content-Length: " + value
                );
            }

            return length;
        } catch (NumberFormatException exception) {
            throw new ServerException(
                "Invalid Content-Length: " + value,
                exception
            );
        }
    }

    /**
     * Extracts the multipart boundary.
     *
     * @return the boundary.
     * @throws ServerException if the boundary parameter is missing or invalid.
     */
    private String getBoundary() throws ServerException {
        final String value = headers
            .getValues()
            .get("Content-Type")
            .getFirst();

        final String[] parts = value.split(";");

        for (int index = 1; index < parts.length; index++) {
            final String part = parts[index].trim();
            final int equals = part.indexOf('=');

            if (equals <= 0) {
                continue;
            }

            final String name = part
                .substring(0, equals)
                .trim();

            if (!name.equalsIgnoreCase("boundary")) {
                continue;
            }

            String boundary = part
                .substring(equals + 1)
                .trim();

            if (
                boundary.length() >= 2
                    && boundary.charAt(0) == '"'
                    && boundary.charAt(boundary.length() - 1) == '"'
            ) {
                boundary = boundary.substring(
                    1,
                    boundary.length() - 1
                );
            }

            if (boundary.isEmpty()) {
                break;
            }

            return boundary;
        }

        throw new ServerException(
            "Multipart boundary is missing"
        );
    }

    /**
     * Checks that exactly one non-empty header with the specified name exists.
     *
     * @param name the header name.
     * @return {@code true} if such a header exists.
     */
    private boolean hasSingleNonEmptyHeader(final String name) {
        final List<String> values =
            headers.getValues().get(name);

        return values != null
            && values.size() == 1
            && !values.getFirst().isBlank();
    }
}