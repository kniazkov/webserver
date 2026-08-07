/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.RequestHeaders;
import com.kniazkov.webserver.ServerException;

/**
 * Parses HTTP request headers.
 */
final class RequestHeadersParser {

    /**
     * Prevents instantiation.
     */
    private RequestHeadersParser() {
    }

    /**
     * Parses HTTP request headers.
     *
     * @param source
     *     the source of HTTP lines.
     * @return
     *     the parsed request headers.
     * @throws ServerException
     *     if the request headers are invalid or incomplete.
     */
    static RequestHeaders parse(final StringSource source)
            throws ServerException {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder();

        parseRequestLine(source, builder);
        parseHeaderLines(source, builder);

        return builder.build();
    }

    /**
     * Parses the HTTP request line.
     *
     * @param source
     *     the source of HTTP lines.
     * @param builder
     *     the request headers builder.
     * @throws ServerException
     *     if the request line is invalid or missing.
     */
    static void parseRequestLine(
        final StringSource source,
        final RequestHeadersBuilder builder
    ) throws ServerException {
        final String line = source.read();

        if (line == null || line.isEmpty()) {
            throw new ServerException("HTTP request line is missing");
        }

        final int firstSpace = line.indexOf(Lexer.SP);
        if (firstSpace <= 0) {
            throw new ServerException("Invalid HTTP request line");
        }

        final int secondSpace = line.indexOf(Lexer.SP, firstSpace + 1);
        if (secondSpace <= firstSpace + 1) {
            throw new ServerException("Invalid HTTP request line");
        }

        if (line.indexOf(Lexer.SP, secondSpace + 1) >= 0) {
            throw new ServerException("Invalid HTTP request line");
        }

        final String method = line.substring(0, firstSpace);
        final String target = line.substring(firstSpace + 1, secondSpace);
        final String version = line.substring(secondSpace + 1);

        if (version.isEmpty()) {
            throw new ServerException("Invalid HTTP request line");
        }

        builder
            .setMethod(HttpMethod.fromString(method))
            .setTarget(target)
            .setVersion(HttpVersion.fromString(version));
    }

    /**
     * Parses HTTP header lines until an empty line is encountered.
     *
     * @param source
     *     the source of HTTP lines.
     * @param builder
     *     the request headers builder.
     * @throws ServerException
     *     if the header section is invalid or incomplete.
     */
    static void parseHeaderLines(
        final StringSource source,
        final RequestHeadersBuilder builder
    ) throws ServerException {
        while (true) {
            final String line = source.read();

            if (line == null) {
                throw new ServerException(
                    "Unexpected end of HTTP headers"
                );
            }

            if (line.isEmpty()) {
                return;
            }

            parseHeaderLine(line, builder);
        }
    }

    /**
     * Parses a single HTTP header line.
     *
     * @param line
     *     the header line.
     * @param builder
     *     the request headers builder.
     * @throws ServerException
     *     if the header line is invalid.
     */
    static void parseHeaderLine(
        final String line,
        final RequestHeadersBuilder builder
    ) throws ServerException {
        final int colon = line.indexOf(':');

        if (colon <= 0) {
            throw new ServerException("Invalid HTTP header: " + line);
        }

        final String name = line.substring(0, colon);
        final String value = trimWhitespace(line, colon + 1);

        builder.addValue(name, value);
    }

    /**
     * Removes optional whitespace from both ends of a header value.
     *
     * @param value
     *     the source string.
     * @param offset
     *     the offset at which the header value begins.
     * @return
     *     the header value without surrounding optional whitespace.
     */
    private static String trimWhitespace(
        final String value,
        final int offset
    ) {
        int start = offset;
        int end = value.length();

        while (
            start < end
                && Lexer.isWhitespace(value.charAt(start))
        ) {
            start++;
        }

        while (
            end > start
                && Lexer.isWhitespace(value.charAt(end - 1))
        ) {
            end--;
        }

        return value.substring(start, end);
    }
}
