/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import java.util.Objects;

/**
 * Parses a complete HTTP/1.x request head into an immutable {@link RequestHeader}.
 *
 * <p>The input must contain exactly one request line, zero or more header fields, and the final
 * empty line, all delimited with CRLF. A request body is not part of the input. This parser does
 * not perform I/O or enforce transport limits; the transport layer must collect a bounded request
 * head before calling {@link #parse(String)}.</p>
 */
public final class RequestHeaderParser {
    /**
     * Prevents instantiation of this stateless utility class.
     */
    private RequestHeaderParser() {
    }

    /**
     * Parses one complete HTTP/1.x request head.
     *
     * @param value request head ending with an empty CRLF-delimited line, never {@code null}
     * @return an immutable parsed request header, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is not a syntactically valid, complete
     * HTTP/1.x request head
     */
    public static RequestHeader parse(final String value) {
        Objects.requireNonNull(value, "request header");

        final int terminatorIndex = value.indexOf("\r\n\r\n");
        if (terminatorIndex < 0) {
            throw createMalformedException("the final empty line is missing");
        }
        if (terminatorIndex != value.length() - 4) {
            throw createMalformedException("data follows the final empty line");
        }
        if (terminatorIndex == 0) {
            throw createMalformedException("the request line is missing");
        }

        final int requestLineEnd = value.indexOf("\r\n");
        final RequestHeader.Builder builder = RequestHeader.createBuilder();
        parseRequestLine(value.substring(0, requestLineEnd), builder);

        int lineStart = requestLineEnd + 2;
        while (lineStart < terminatorIndex) {
            final int lineEnd = value.indexOf("\r\n", lineStart);
            parseHeaderField(value.substring(lineStart, lineEnd), builder);
            lineStart = lineEnd + 2;
        }
        return builder.create();
    }

    /**
     * Parses the request line into a builder.
     *
     * @param line request line without its CRLF delimiter, never {@code null}
     * @param builder destination builder, never {@code null}
     * @throws IllegalArgumentException if the request line is malformed
     */
    private static void parseRequestLine(
            final String line,
            final RequestHeader.Builder builder) {
        final int firstSpace = line.indexOf(' ');
        final int secondSpace = line.indexOf(' ', firstSpace + 1);
        if (firstSpace <= 0
                || secondSpace <= firstSpace + 1
                || secondSpace == line.length() - 1
                || line.indexOf(' ', secondSpace + 1) >= 0) {
            throw createMalformedException("the request line must contain exactly three parts");
        }

        builder.setMethod(line.substring(0, firstSpace));
        builder.setRequestTarget(line.substring(firstSpace + 1, secondSpace));
        parseHttpVersion(line.substring(secondSpace + 1), builder);
    }

    /**
     * Parses the strict HTTP-name and single-digit version syntax used by HTTP/1.x.
     *
     * @param value HTTP version token, never {@code null}
     * @param builder destination builder, never {@code null}
     * @throws IllegalArgumentException if {@code value} is not an HTTP version token
     */
    private static void parseHttpVersion(
            final String value,
            final RequestHeader.Builder builder) {
        if (value.length() != 8
                || !value.startsWith("HTTP/")
                || value.charAt(6) != '.'
                || !isAsciiDigit(value.charAt(5))
                || !isAsciiDigit(value.charAt(7))) {
            throw createMalformedException("the HTTP version is malformed");
        }
        builder.setHttpVersion(value.charAt(5) - '0', value.charAt(7) - '0');
    }

    /**
     * Determines whether a character is an ASCII digit.
     *
     * @param value character to inspect
     * @return {@code true} when {@code value} is between {@code 0} and {@code 9}
     */
    private static boolean isAsciiDigit(final char value) {
        return value >= '0' && value <= '9';
    }

    /**
     * Parses one header field into a builder.
     *
     * @param line header field without its CRLF delimiter, never {@code null}
     * @param builder destination builder, never {@code null}
     * @throws IllegalArgumentException if the header field is malformed
     */
    private static void parseHeaderField(
            final String line,
            final RequestHeader.Builder builder) {
        if (line.isEmpty()) {
            throw createMalformedException("an unexpected empty header field was found");
        }
        if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
            throw createMalformedException("obsolete folded header fields are not supported");
        }

        final int colonIndex = line.indexOf(':');
        if (colonIndex <= 0) {
            throw createMalformedException("a header field is missing its name or colon");
        }

        final String name = line.substring(0, colonIndex);
        final String value = trimOptionalWhitespace(line.substring(colonIndex + 1));
        builder.addHeader(name, value);
    }

    /**
     * Removes optional spaces and horizontal tabs surrounding a header field value.
     *
     * @param value raw header field value, never {@code null}
     * @return the value without surrounding optional whitespace, never {@code null}
     */
    private static String trimOptionalWhitespace(final String value) {
        int start = 0;
        while (start < value.length() && isOptionalWhitespace(value.charAt(start))) {
            start++;
        }

        int end = value.length();
        while (end > start && isOptionalWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    /**
     * Determines whether a character is HTTP optional whitespace.
     *
     * @param value character to inspect
     * @return {@code true} for a space or horizontal tab
     */
    private static boolean isOptionalWhitespace(final char value) {
        return value == ' ' || value == '\t';
    }

    /**
     * Creates a consistently worded malformed-input exception.
     *
     * @param reason description of the syntax violation, never {@code null}
     * @return a new exception, never {@code null}
     */
    private static IllegalArgumentException createMalformedException(final String reason) {
        return new IllegalArgumentException("Malformed request header: " + reason);
    }
}
