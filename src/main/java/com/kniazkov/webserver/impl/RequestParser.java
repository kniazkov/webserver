/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.RequestPath;
import com.kniazkov.webserver.ServerException;

import java.io.IOException;
import java.io.InputStream;
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
     * The request body storage until ownership is transferred to a request.
     */
    private StoredUploadedData body;

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
        try {
            return new RequestParser(source, options).parse();
        } catch (
            ConnectionClosedException
                | ConnectionTimeoutException exception
        ) {
            throw exception;
        } catch (ServerException exception) {
            final HttpStatus status = exception
                .getStatus()
                .orElse(HttpStatus.BAD_REQUEST);

            // Parser diagnostics can contain request data. Keep them as the
            // server-side cause and expose only the standard status reason.
            throw new ServerException(
                status,
                status.getReason(),
                exception
            );
        }
    }

    /**
     * Parses the complete request.
     *
     * @return the parsed request.
     * @throws ServerException if the request is invalid.
     */
    private Request parse() throws ServerException {
        try {
            parseHeaders();
            validateVersion();
            parseTarget();
            parseCookies();
            parseBody();

            final Request result = builder.build();
            body = null;
            return result;
        } catch (ServerException exception) {
            if (body != null) {
                try {
                    body.close();
                } catch (ServerException cleanup) {
                    exception.addSuppressed(cleanup);
                }
            }

            throw exception;
        }
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
                HttpStatus.NOT_IMPLEMENTED,
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

        final long remainingLimit = options.getMaxRequestSize()
            - source.getCount();

        if (contentLength > remainingLimit) {
            throw new ServerException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Maximum HTTP request size exceeded"
            );
        }

        body = UploadedDataReader.read(
            source,
            contentLength,
            options.getMaxInMemoryBodySize()
        );

        builder.setBody(body);

        final ContentType contentType = getContentType();

        if (headers.getMethod() == HttpMethod.POST) {
            if (contentType == ContentType.MULTIPART_FORM_DATA) {
                parseMultipart();
            } else if (
                contentType
                    == ContentType.APPLICATION_FORM_URLENCODED
            ) {
                parseUrlEncodedForm();
            }
        }
    }

    /**
     * Parses the stored request body as multipart form data.
     *
     * @throws ServerException
     *     if the multipart body is invalid.
     */
    private void parseMultipart() throws ServerException {
        try (InputStream input = body.openStream()) {
            MultipartParser.parse(
                new InputStreamByteSource(input),
                body,
                getBoundary(),
                options,
                builder
            );
        } catch (IOException exception) {
            throw new ServerException(
                "Cannot close uploaded request data",
                exception
            );
        }
    }

    /**
     * Parses the stored request body as URL-encoded form data.
     *
     * @throws ServerException
     *     if the form is invalid or too large.
     */
    private void parseUrlEncodedForm() throws ServerException {
        if (body.getSize() > options.getMaxFormSize()) {
            throw new ServerException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Maximum form data size exceeded"
            );
        }

        UrlEncodedParser.parseForm(
            body.readAllBytes(),
            builder
        );
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
