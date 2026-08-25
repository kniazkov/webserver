/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpStatus;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ServerException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses multipart form data.
 */
final class MultipartParser {

    /**
     * The byte source.
     */
    private final ByteSource source;

    /**
     * The complete stored request body.
     */
    private final StoredUploadedData data;

    /**
     * The multipart boundary.
     */
    private final byte[] boundary;

    /**
     * The boundary used after part data.
     */
    private final byte[] dataBoundary;

    /**
     * The server options.
     */
    private final Options options;

    /**
     * The request builder.
     */
    private final RequestBuilder builder;

    /**
     * The number of body bytes consumed by this parser.
     */
    private long position;

    /**
     * The total number of decoded form field bytes.
     */
    private long formSize;

    /**
     * Creates a multipart parser.
     *
     * @param source
     *     the byte source.
     * @param data
     *     the stored request body.
     * @param boundary
     *     the multipart boundary.
     * @param options
     *     the server options.
     * @param builder
     *     the request builder.
     */
    private MultipartParser(
        final ByteSource source,
        final StoredUploadedData data,
        final String boundary,
        final Options options,
        final RequestBuilder builder
    ) {
        this.source = source;
        this.data = data;
        this.boundary = bytes("--" + boundary);
        this.dataBoundary = bytes("\r\n--" + boundary);
        this.options = options;
        this.builder = builder;
    }

    /**
     * Parses multipart form data.
     *
     * @param source
     *     the byte source.
     * @param data
     *     the stored request body.
     * @param boundary
     *     the multipart boundary.
     * @param options
     *     the server options.
     * @param builder
     *     the request builder.
     * @throws ServerException
     *     if the multipart data is invalid.
     */
    static void parse(
        final ByteSource source,
        final StoredUploadedData data,
        final String boundary,
        final Options options,
        final RequestBuilder builder
    ) throws ServerException {
        if (boundary == null || boundary.isEmpty()) {
            throw new ServerException("Multipart boundary is missing");
        }

        new MultipartParser(
            source,
            data,
            boundary,
            options,
            builder
        ).parse();
    }

    /**
     * Parses multipart data.
     *
     * @throws ServerException
     *     if the multipart data is invalid.
     */
    private void parse() throws ServerException {
        readInitialBoundary();

        while (true) {
            final PartHeaders headers = readPartHeaders();
            final PartData part = readPartData(
                headers.filename != null
            );

            if (headers.filename == null) {
                builder.addForm(
                    headers.name,
                    new String(
                        part.field,
                        StandardCharsets.UTF_8
                    )
                );
            } else {
                builder.addFile(
                    headers.name,
                    new UploadedFileImpl(
                        headers.filename,
                        headers.contentType,
                        data.slice(part.offset, part.length)
                    )
                );
            }

            if (part.last) {
                return;
            }
        }
    }

    /**
     * Reads and validates the initial multipart boundary.
     *
     * @throws ServerException
     *     if the initial boundary is invalid.
     */
    private void readInitialBoundary() throws ServerException {
        for (byte expected : boundary) {
            final int value = read();

            if (value == -1 || (byte) value != expected) {
                throw new ServerException(
                    "Invalid multipart initial boundary"
                );
            }
        }

        require(Lexer.CR);
        require(Lexer.LF);
    }

    /**
     * Reads headers of one multipart part.
     *
     * @return
     *     the parsed part headers.
     * @throws ServerException
     *     if the part headers are invalid.
     */
    private PartHeaders readPartHeaders() throws ServerException {
        final Map<String, String> headers = new LinkedHashMap<>();

        while (true) {
            final String line = readLine();

            if (line.isEmpty()) {
                break;
            }

            final int colon = line.indexOf(':');

            if (colon <= 0) {
                throw new ServerException(
                    "Invalid multipart header: " + line
                );
            }

            final String name = Lexer.canonicalizeHeaderName(
                line.substring(0, colon)
            );

            final String value = trimWhitespace(
                line.substring(colon + 1)
            );

            if (
                (name.equals("Content-Disposition")
                    || name.equals("Content-Type"))
                    && headers.containsKey(name)
            ) {
                throw new ServerException(
                    "Duplicate multipart header: " + name
                );
            }

            headers.put(name, value);
        }

        final String disposition = headers.get(
            "Content-Disposition"
        );

        if (disposition == null) {
            throw new ServerException(
                "Multipart Content-Disposition is missing"
            );
        }

        final Map<String, String> parameters =
            parseContentDisposition(disposition);

        final String name = parameters.get("name");

        if (name == null || name.isEmpty()) {
            throw new ServerException(
                "Multipart field name is missing"
            );
        }

        final String filename = parameters.get("filename");

        if (filename != null && filename.isEmpty()) {
            throw new ServerException(
                "Multipart file name is missing"
            );
        }

        final ContentType contentType = ContentType.fromString(
            headers.get("Content-Type")
        );

        return new PartHeaders(
            name,
            filename,
            contentType
        );
    }

    /**
     * Reads data of one multipart part.
     *
     * @param file
     *     whether the part contains an uploaded file.
     * @return
     *     the part data and information about the following boundary.
     * @throws ServerException
     *     if the source ends unexpectedly or the file is too large.
     */
    private PartData readPartData(final boolean file)
        throws ServerException {
        final long start = position;
        final ByteAccumulator accumulator = file
            ? null
            : new ByteAccumulator();

        final BoundaryWindow window = new BoundaryWindow(
            dataBoundary.length
        );

        while (true) {
            final int value = read();

            if (value == -1) {
                throw new ServerException(
                    "Unexpected end of multipart data"
                );
            }

            window.append(value);

            if (accumulator != null) {
                accumulator.append(value);
            }

            validatePartSize(
                position - start - dataBoundary.length,
                file
            );

            if (!window.endsWith(dataBoundary)) {
                continue;
            }

            final int first = read();

            if (first == -1) {
                throw new ServerException(
                    "Unexpected end of multipart data"
                );
            }

            final int second = read();

            if (second == -1) {
                throw new ServerException(
                    "Unexpected end of multipart data"
                );
            }

            if (
                first == Lexer.CR
                    && second == Lexer.LF
            ) {
                return createPartData(
                    accumulator,
                    start,
                    false,
                    file
                );
            }

            if (
                first == '-'
                    && second == '-'
            ) {
                return createPartData(
                    accumulator,
                    start,
                    true,
                    file
                );
            }

            /*
             * The sequence only looked like a boundary.
             * Both following bytes are ordinary part data.
             */
            window.append(first);
            window.append(second);

            if (accumulator != null) {
                accumulator.append(first);
                accumulator.append(second);
            }
        }
    }

    /**
     * Creates parsed part data after a boundary has been consumed.
     *
     * @param accumulator
     *     the form field data, or {@code null} for an uploaded file.
     * @param start
     *     the absolute part data offset.
     * @param last
     *     whether the final boundary was found.
     * @param file
     *     whether the part contains an uploaded file.
     * @return
     *     the parsed part data.
     * @throws ServerException
     *     if the part exceeds its configured limit.
     */
    private PartData createPartData(
        final ByteAccumulator accumulator,
        final long start,
        final boolean last,
        final boolean file
    ) throws ServerException {
        final long length = position
            - start
            - dataBoundary.length
            - 2;

        validatePartSize(length, file);

        final byte[] field;

        if (accumulator == null) {
            field = null;
        } else {
            accumulator.removeLast(dataBoundary.length);
            field = accumulator.toByteArray();
            formSize += field.length;
        }

        return new PartData(
            start,
            length,
            field,
            last
        );
    }

    /**
     * Checks a part against its configured limit.
     *
     * @param length
     *     the possible part data length.
     * @param file
     *     whether the part is an uploaded file.
     * @throws ServerException
     *     if the configured limit is exceeded.
     */
    private void validatePartSize(
        final long length,
        final boolean file
    ) throws ServerException {
        if (length <= 0) {
            return;
        }

        if (file && length > options.getMaxFileSize()) {
            throw new ServerException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Maximum uploaded file size exceeded"
            );
        }

        if (
            !file
                && length > options.getMaxFormSize() - formSize
        ) {
            throw new ServerException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Maximum form data size exceeded"
            );
        }
    }

    /**
     * Reads a CRLF-terminated multipart header line.
     *
     * @return
     *     the line without CRLF.
     * @throws ServerException
     *     if the line is invalid or incomplete.
     */
    private String readLine() throws ServerException {
        final StringBuilder builder = new StringBuilder();

        while (true) {
            final int value = read();

            if (value == -1) {
                throw new ServerException(
                    "Unexpected end of multipart headers"
                );
            }

            if (value == Lexer.LF) {
                throw new ServerException(
                    "Invalid multipart line ending"
                );
            }

            if (value == Lexer.CR) {
                require(Lexer.LF);
                return builder.toString();
            }

            builder.append((char) value);
        }
    }

    /**
     * Parses a Content-Disposition header.
     *
     * @param value
     *     the header value.
     * @return
     *     the disposition parameters.
     * @throws ServerException
     *     if the disposition is invalid.
     */
    private static Map<String, String> parseContentDisposition(
        final String value
    ) throws ServerException {
        final Map<String, String> result = new LinkedHashMap<>();

        int index = 0;

        while (
            index < value.length()
                && Lexer.isWhitespace(value.charAt(index))
        ) {
            index++;
        }

        final int typeStart = index;

        while (
            index < value.length()
                && value.charAt(index) != ';'
        ) {
            index++;
        }

        final String type = trimWhitespace(
            value.substring(typeStart, index)
        );

        if (!type.equalsIgnoreCase("form-data")) {
            throw new ServerException(
                "Invalid multipart Content-Disposition"
            );
        }

        while (index < value.length()) {
            index++;

            while (
                index < value.length()
                    && Lexer.isWhitespace(value.charAt(index))
            ) {
                index++;
            }

            final int nameStart = index;

            while (
                index < value.length()
                    && value.charAt(index) != '='
                    && value.charAt(index) != ';'
            ) {
                index++;
            }

            if (
                index == nameStart
                    || index == value.length()
                    || value.charAt(index) != '='
            ) {
                throw new ServerException(
                    "Invalid multipart Content-Disposition"
                );
            }

            final String name = trimWhitespace(
                value.substring(nameStart, index)
            );

            if (name.isEmpty()) {
                throw new ServerException(
                    "Invalid multipart Content-Disposition"
                );
            }

            index++;

            while (
                index < value.length()
                    && Lexer.isWhitespace(value.charAt(index))
            ) {
                index++;
            }

            final String parameterValue;

            if (
                index < value.length()
                    && value.charAt(index) == '"'
            ) {
                index++;
                final StringBuilder builder = new StringBuilder();
                boolean closed = false;

                while (index < value.length()) {
                    final char ch = value.charAt(index++);

                    if (ch == '"') {
                        closed = true;
                        break;
                    }

                    if (ch == '\\') {
                        if (index == value.length()) {
                            throw new ServerException(
                                "Invalid multipart Content-Disposition"
                            );
                        }

                        builder.append(value.charAt(index++));
                    } else {
                        builder.append(ch);
                    }
                }

                if (!closed) {
                    throw new ServerException(
                        "Invalid multipart Content-Disposition"
                    );
                }

                parameterValue = builder.toString();

                while (
                    index < value.length()
                        && Lexer.isWhitespace(value.charAt(index))
                ) {
                    index++;
                }

                if (
                    index < value.length()
                        && value.charAt(index) != ';'
                ) {
                    throw new ServerException(
                        "Invalid multipart Content-Disposition"
                    );
                }
            } else {
                final int valueStart = index;

                while (
                    index < value.length()
                        && value.charAt(index) != ';'
                ) {
                    index++;
                }

                parameterValue = trimWhitespace(
                    value.substring(valueStart, index)
                );

                if (parameterValue.isEmpty()) {
                    throw new ServerException(
                        "Invalid multipart Content-Disposition"
                    );
                }
            }

            result.put(name, parameterValue);
        }

        return result;
    }

    /**
     * Reads and validates one expected byte.
     *
     * @param expected
     *     the expected byte.
     * @throws ServerException
     *     if another byte is received.
     */
    private void require(final int expected)
        throws ServerException {
        if (read() != expected) {
            throw new ServerException(
                "Invalid multipart data"
            );
        }
    }

    /**
     * Reads one byte and advances the absolute body position.
     *
     * @return
     *     the next byte, or {@code -1} at the end of the body.
     * @throws ServerException
     *     if reading fails.
     */
    private int read() throws ServerException {
        final int value = source.read();

        if (value >= 0) {
            position++;
        }

        return value;
    }

    /**
     * Removes HTTP optional whitespace from both ends.
     *
     * @param value
     *     the source value.
     * @return
     *     the trimmed value.
     */
    private static String trimWhitespace(final String value) {
        int start = 0;
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

    /**
     * Converts ASCII text to bytes.
     *
     * @param value
     *     the text.
     * @return
     *     the bytes.
     */
    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Parsed headers of one multipart part.
     */
    private static final class PartHeaders {

        /**
         * The form field name.
         */
        private final String name;

        /**
         * The uploaded file name, or {@code null}.
         */
        private final String filename;

        /**
         * The content type.
         */
        private final ContentType contentType;

        /**
         * Creates part headers.
         *
         * @param name
         *     the field name.
         * @param filename
         *     the file name.
         * @param contentType
         *     the content type.
         */
        private PartHeaders(
            final String name,
            final String filename,
            final ContentType contentType
        ) {
            this.name = name;
            this.filename = filename;
            this.contentType = contentType;
        }
    }

    /**
     * Parsed data of one multipart part.
     */
    private static final class PartData {

        /**
         * The absolute part data offset.
         */
        private final long offset;

        /**
         * The part data length.
         */
        private final long length;

        /**
         * Decoded form field bytes, or {@code null} for a file.
         */
        private final byte[] field;

        /**
         * Whether the part is followed by the final boundary.
         */
        private final boolean last;

        /**
         * Creates parsed part data.
         *
         * @param offset
         *     the absolute part data offset.
         * @param length
         *     the part data length.
         * @param field
         *     the form field bytes.
         * @param last
         *     whether this is the last part.
         */
        private PartData(
            final long offset,
            final long length,
            final byte[] field,
            final boolean last
        ) {
            this.offset = offset;
            this.length = length;
            this.field = field;
            this.last = last;
        }
    }

    /**
     * Fixed-size rolling window used for boundary detection.
     */
    private static final class BoundaryWindow {

        /**
         * The rolling bytes.
         */
        private final byte[] data;

        /**
         * The index at which the next byte is written.
         */
        private int cursor;

        /**
         * The number of available bytes.
         */
        private int size;

        /**
         * Creates a rolling window.
         *
         * @param length
         *     the window length.
         */
        BoundaryWindow(final int length) {
            data = new byte[length];
        }

        /**
         * Adds one byte.
         *
         * @param value
         *     the byte value.
         */
        void append(final int value) {
            data[cursor] = (byte) value;
            cursor = (cursor + 1) % data.length;

            if (size < data.length) {
                size++;
            }
        }

        /**
         * Returns whether this window ends with the pattern.
         *
         * @param pattern
         *     the expected bytes.
         * @return
         *     whether the bytes match.
         */
        boolean endsWith(final byte[] pattern) {
            if (size != pattern.length) {
                return false;
            }

            for (int index = 0; index < pattern.length; index++) {
                if (
                    data[(cursor + index) % data.length]
                        != pattern[index]
                ) {
                    return false;
                }
            }

            return true;
        }
    }
}
