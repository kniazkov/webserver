/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

/**
 * Parses the header section of an HTTP request line by line.
 * <p>
 * Each supplied line must not contain CR or LF characters. The first line is
 * interpreted as the request line. Subsequent non-empty lines are interpreted
 * as HTTP header fields. An empty line marks the end of the header section.
 */
final class RequestHeaderParser {

    /**
     * The builder populated by this parser.
     */
    private final RequestHeader.Builder builder;

    /**
     * The current parser state.
     */
    private State state = State.REQUEST_LINE;

    /**
     * Creates a parser that populates the specified builder.
     *
     * @param builder
     *     the request header builder.
     * @throws IllegalArgumentException
     *     if the builder is {@code null}.
     */
    public RequestHeaderParser(final RequestHeader.Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder must not be null.");
        }
        this.builder = builder;
    }

    /**
     * Parses the next line of the HTTP request header.
     *
     * @param line
     *     the next line without CR or LF characters.
     * @throws ServerException
     *     if the line is invalid or appears after the end of the header.
     */
    public void parseLine(final String line) throws ServerException {
        validateLine(line);

        switch (state) {
            case REQUEST_LINE -> {
                parseRequestLine(line);
                state = State.HEADER_FIELDS;
            }
            case HEADER_FIELDS -> {
                if (line.isEmpty()) {
                    state = State.FINISHED;
                } else {
                    parseHeaderField(line);
                }
            }
            case FINISHED -> throw new ServerException(
                "Unexpected data after the end of the request header."
            );
        }
    }

    /**
     * Returns whether the complete request header has been parsed.
     *
     * @return
     *     {@code true} if an empty terminating line has been parsed;
     *     otherwise, {@code false}.
     */
    public boolean isFinished() {
        return state == State.FINISHED;
    }

    /**
     * Parses the HTTP request line.
     *
     * @param line
     *     the request line.
     * @throws ServerException
     *     if the request line is malformed.
     */
    private void parseRequestLine(final String line) throws ServerException {
        if (line.isEmpty()) {
            throw new ServerException("Request line must not be empty.");
        }

        final String[] parts = line.split(" ", -1);
        if (parts.length != 3
            || parts[0].isEmpty()
            || parts[1].isEmpty()
            || parts[2].isEmpty()) {
            throw new ServerException("Malformed HTTP request line: " + line);
        }

        builder
            .setMethod(HttpMethod.fromString(parts[0]))
            .setTarget(parts[1])
            .setVersion(HttpVersion.fromString(parts[2]));
    }

    /**
     * Parses an HTTP header field.
     *
     * @param line
     *     the header field line.
     * @throws ServerException
     *     if the header field is malformed.
     */
    private void parseHeaderField(final String line) throws ServerException {
        if (Character.isWhitespace(line.charAt(0))) {
            throw new ServerException(
                "Header continuation lines are not supported: " + line
            );
        }

        final int separatorIndex = line.indexOf(':');
        if (separatorIndex < 0) {
            throw new ServerException(
                "Header field does not contain a colon: " + line
            );
        }

        final String name = line.substring(0, separatorIndex).trim();
        final String value = line.substring(separatorIndex + 1).trim();

        if (name.isEmpty()) {
            throw new ServerException("Header field name must not be empty.");
        }

        builder.addValue(name, value);
    }

    /**
     * Validates a line supplied to the parser.
     *
     * @param line
     *     the line to validate.
     * @throws ServerException
     *     if the line is {@code null} or contains CR or LF characters.
     */
    private void validateLine(final String line) throws ServerException {
        if (line == null) {
            throw new ServerException("HTTP header line must not be null.");
        }
        if (line.indexOf('\r') >= 0 || line.indexOf('\n') >= 0) {
            throw new ServerException(
                "HTTP header line must not contain CR or LF characters."
            );
        }
    }

    /**
     * Represents the current stage of HTTP request header parsing.
     */
    private enum State {

        /**
         * The parser expects the HTTP request line.
         */
        REQUEST_LINE,

        /**
         * The parser expects HTTP header fields or an empty terminating line.
         */
        HEADER_FIELDS,

        /**
         * The complete HTTP request header has been parsed.
         */
        FINISHED
    }
}
