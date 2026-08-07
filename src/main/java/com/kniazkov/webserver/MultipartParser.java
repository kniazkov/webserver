/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parses multipart form data incrementally.
 * <p>
 * The parser accepts arbitrary portions of the HTTP request body. Multipart
 * boundaries and part headers may be split between several input arrays.
 * Parsed form values and uploaded files are added directly to the supplied
 * {@link Request.Builder}.
 * <p>
 * This parser is not thread-safe.
 */
final class MultipartParser {

    /**
     * The CRLF byte sequence.
     */
    private static final byte[] CRLF = new byte[] {'\r', '\n'};

    /**
     * The byte sequence separating part headers from part data.
     */
    private static final byte[] HEADER_SEPARATOR =
        new byte[] {'\r', '\n', '\r', '\n'};

    /**
     * The request builder populated by this parser.
     */
    private final Request.Builder builder;

    /**
     * The server options containing multipart size limits.
     */
    private final Options options;

    /**
     * The first multipart boundary.
     */
    private final byte[] firstBoundary;

    /**
     * The boundary preceding every part after the first one.
     */
    private final byte[] partBoundary;

    /**
     * The unprocessed bytes retained between parser invocations.
     */
    private byte[] pending = new byte[0];

    /**
     * The data of the part currently being parsed.
     */
    private final ByteArrayOutputStream partData =
        new ByteArrayOutputStream();

    /**
     * The current parser state.
     */
    private State state = State.FIRST_BOUNDARY;

    /**
     * The total number of bytes supplied to the parser.
     */
    private long receivedSize;

    /**
     * The name of the current form field.
     */
    private String fieldName;

    /**
     * The original name of the current uploaded file.
     */
    private String fileName;

    /**
     * The content type of the current uploaded file.
     */
    private ContentType contentType;

    /**
     * Creates a multipart parser.
     *
     * @param builder
     *     the request builder populated by this parser.
     * @param boundary
     *     the multipart boundary without the leading double hyphen.
     * @param options
     *     the server options containing request and file size limits.
     * @throws IllegalArgumentException
     *     if the boundary is empty.
     * @throws NullPointerException
     *     if any argument is {@code null}.
     */
    public MultipartParser(
        final Request.Builder builder,
        final String boundary,
        final Options options
    ) {
        this.builder = Objects.requireNonNull(
            builder,
            "Request builder must not be null."
        );
        this.options = Objects.requireNonNull(
            options,
            "Server options must not be null."
        );

        final String value = Objects.requireNonNull(
            boundary,
            "Multipart boundary must not be null."
        );

        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                "Multipart boundary must not be empty."
            );
        }

        this.firstBoundary = ("--" + value).getBytes(
            StandardCharsets.ISO_8859_1
        );
        this.partBoundary = ("\r\n--" + value).getBytes(
            StandardCharsets.ISO_8859_1
        );
    }

    /**
     * Accepts the next portion of multipart data.
     *
     * @param data
     *     the next portion of the HTTP request body.
     * @return
     *     {@code true} if additional data is required, or {@code false} if
     *     the final multipart boundary has been parsed.
     * @throws ServerException
     *     if the multipart data is malformed or exceeds a configured limit.
     * @throws NullPointerException
     *     if the supplied data is {@code null}.
     */
    public boolean accept(final byte[] data) throws ServerException {
        Objects.requireNonNull(data, "Multipart data must not be null.");

        if (state == State.FINISHED) {
            throw new ServerException(
                "Multipart data has already been completely parsed."
            );
        }

        receivedSize += data.length;
        if (receivedSize > options.getMaxRequestSize()) {
            throw new ServerException(
                "Maximum request size has been exceeded."
            );
        }

        pending = concatenate(pending, data);

        boolean progressed;
        do {
            progressed = switch (state) {
                case FIRST_BOUNDARY -> parseFirstBoundary();
                case HEADERS -> parseHeaders();
                case DATA -> parsePartData();
                case BOUNDARY_SUFFIX -> parseBoundarySuffix();
                case FINISHED -> false;
            };
        } while (progressed && state != State.FINISHED);

        return state != State.FINISHED;
    }

    /**
     * Returns whether the complete multipart body has been parsed.
     *
     * @return
     *     {@code true} if the final multipart boundary has been parsed.
     */
    public boolean isFinished() {
        return state == State.FINISHED;
    }

    /**
     * Parses the first multipart boundary.
     *
     * @return
     *     {@code true} if parser state has progressed.
     * @throws ServerException
     *     if the first boundary is malformed.
     */
    private boolean parseFirstBoundary() throws ServerException {
        if (pending.length < firstBoundary.length) {
            validateBoundaryPrefix(pending, firstBoundary);
            return false;
        }

        if (!startsWith(pending, firstBoundary)) {
            throw new ServerException(
                "Multipart body does not start with the expected boundary."
            );
        }

        pending = removePrefix(pending, firstBoundary.length);
        state = State.BOUNDARY_SUFFIX;
        return true;
    }

    /**
     * Parses the headers of the current multipart part.
     *
     * @return
     *     {@code true} if complete part headers were parsed.
     * @throws ServerException
     *     if the part headers are malformed.
     */
    private boolean parseHeaders() throws ServerException {
        final int separator = indexOf(pending, HEADER_SEPARATOR);
        if (separator < 0) {
            return false;
        }

        final String headerText = new String(
            pending,
            0,
            separator,
            StandardCharsets.ISO_8859_1
        );

        pending = removePrefix(
            pending,
            separator + HEADER_SEPARATOR.length
        );

        parsePartHeaders(headerText);
        partData.reset();
        state = State.DATA;
        return true;
    }

    /**
     * Parses data belonging to the current multipart part.
     *
     * @return
     *     {@code true} if a boundary was found or safe bytes were consumed.
     * @throws ServerException
     *     if the current uploaded file exceeds its configured size limit.
     */
    private boolean parsePartData() throws ServerException {
        final int boundaryIndex = indexOf(pending, partBoundary);

        if (boundaryIndex >= 0) {
            appendPartData(pending, 0, boundaryIndex);
            finishPart();

            pending = removePrefix(
                pending,
                boundaryIndex + partBoundary.length
            );
            state = State.BOUNDARY_SUFFIX;
            return true;
        }

        final int retainedLength = Math.min(
            pending.length,
            partBoundary.length - 1
        );
        final int consumedLength = pending.length - retainedLength;

        if (consumedLength == 0) {
            return false;
        }

        appendPartData(pending, 0, consumedLength);
        pending = removePrefix(pending, consumedLength);
        return true;
    }

    /**
     * Parses the bytes following a multipart boundary.
     *
     * @return
     *     {@code true} if the suffix was parsed.
     * @throws ServerException
     *     if the boundary suffix is malformed.
     */
    private boolean parseBoundarySuffix() throws ServerException {
        if (pending.length < 2) {
            return false;
        }

        if (pending[0] == '-' && pending[1] == '-') {
            pending = removePrefix(pending, 2);
            state = State.FINISHED;
            return true;
        }

        if (pending[0] == '\r' && pending[1] == '\n') {
            pending = removePrefix(pending, CRLF.length);
            state = State.HEADERS;
            return true;
        }

        throw new ServerException(
            "Malformed multipart boundary suffix."
        );
    }

    /**
     * Parses the headers of a multipart part.
     *
     * @param text
     *     the textual part header section.
     * @throws ServerException
     *     if required headers or parameters are missing.
     */
    private void parsePartHeaders(final String text)
        throws ServerException {

        final Map<String, String> headers = new LinkedHashMap<>();

        if (!text.isEmpty()) {
            for (String line : text.split("\\r\\n", -1)) {
                final int separator = line.indexOf(':');

                if (separator <= 0) {
                    throw new ServerException(
                        "Malformed multipart part header: " + line
                    );
                }

                final String name = line.substring(0, separator).trim();
                final String value = line.substring(separator + 1).trim();

                headers.put(name.toLowerCase(), value);
            }
        }

        final String disposition = headers.get(
            HttpHeaders.CONTENT_DISPOSITION.toLowerCase()
        );

        if (disposition == null) {
            throw new ServerException(
                "Multipart part does not contain Content-Disposition."
            );
        }

        final Map<String, String> parameters =
            parseDisposition(disposition);

        fieldName = parameters.get("name");
        fileName = parameters.get("filename");
        if (headers.containsKey(HttpHeaders.CONTENT_TYPE.toLowerCase())) {
            contentType = ContentType.fromString(
                headers.get(HttpHeaders.CONTENT_TYPE.toLowerCase())
            );
        } else {
            contentType = ContentType.APPLICATION_OCTET_STREAM;
        }

        if (fieldName == null || fieldName.isEmpty()) {
            throw new ServerException(
                "Multipart part does not contain a field name."
            );
        }
    }

    /**
     * Parses parameters from a Content-Disposition header value.
     *
     * @param value
     *     the Content-Disposition header value.
     * @return
     *     the parsed parameters.
     */
    private Map<String, String> parseDisposition(final String value) {
        final Map<String, String> result = new LinkedHashMap<>();
        final String[] elements = value.split(";");

        for (int index = 1; index < elements.length; index++) {
            final String element = elements[index].trim();
            final int separator = element.indexOf('=');

            if (separator <= 0) {
                continue;
            }

            final String name = element.substring(0, separator)
                .trim()
                .toLowerCase();

            String parameterValue = element.substring(separator + 1).trim();

            if (parameterValue.length() >= 2
                && parameterValue.startsWith("\"")
                && parameterValue.endsWith("\"")) {
                parameterValue = parameterValue.substring(
                    1,
                    parameterValue.length() - 1
                );
            }

            result.put(name, parameterValue);
        }

        return result;
    }

    /**
     * Appends bytes to the data of the current multipart part.
     *
     * @param source
     *     the source byte array.
     * @param offset
     *     the source offset.
     * @param length
     *     the number of bytes to append.
     * @throws ServerException
     *     if the current file exceeds its configured size limit.
     */
    private void appendPartData(
        final byte[] source,
        final int offset,
        final int length
    ) throws ServerException {
        if (length == 0) {
            return;
        }

        if (fileName != null
            && (long) partData.size() + length
            > options.getMaxFileSize()) {
            throw new ServerException(
                "Maximum uploaded file size has been exceeded."
            );
        }

        partData.write(source, offset, length);
    }

    /**
     * Adds the completely parsed part to the request builder.
     */
    private void finishPart() {
        final byte[] data = partData.toByteArray();

        if (fileName == null) {
            builder.addFormValue(
                fieldName,
                new String(data, StandardCharsets.UTF_8)
            );
        } else {
            builder.addFile(
                fieldName,
                new UploadedFile(fileName, contentType, data)
            );
        }

        fieldName = null;
        fileName = null;
        contentType = null;
        partData.reset();
    }

    /**
     * Validates that the specified data is a prefix of a boundary.
     *
     * @param data
     *     the supplied boundary prefix.
     * @param boundary
     *     the expected complete boundary.
     * @throws ServerException
     *     if the data does not match the expected boundary prefix.
     */
    private void validateBoundaryPrefix(
        final byte[] data,
        final byte[] boundary
    ) throws ServerException {
        for (int index = 0; index < data.length; index++) {
            if (data[index] != boundary[index]) {
                throw new ServerException(
                    "Multipart body starts with an invalid boundary."
                );
            }
        }
    }

    /**
     * Returns the position of a byte sequence inside another byte sequence.
     *
     * @param source
     *     the source byte array.
     * @param target
     *     the sequence to locate.
     * @return
     *     the target position, or {@code -1} if it was not found.
     */
    private static int indexOf(
        final byte[] source,
        final byte[] target
    ) {
        if (target.length == 0) {
            return 0;
        }

        final int maximum = source.length - target.length;

        for (int index = 0; index <= maximum; index++) {
            boolean matches = true;

            for (int offset = 0; offset < target.length; offset++) {
                if (source[index + offset] != target[offset]) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Returns whether a byte array starts with the specified prefix.
     *
     * @param source
     *     the source byte array.
     * @param prefix
     *     the expected prefix.
     * @return
     *     {@code true} if the prefix matches.
     */
    private static boolean startsWith(
        final byte[] source,
        final byte[] prefix
    ) {
        if (source.length < prefix.length) {
            return false;
        }

        for (int index = 0; index < prefix.length; index++) {
            if (source[index] != prefix[index]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Concatenates two byte arrays.
     *
     * @param first
     *     the first byte array.
     * @param second
     *     the second byte array.
     * @return
     *     the concatenated byte array.
     */
    private static byte[] concatenate(
        final byte[] first,
        final byte[] second
    ) {
        final byte[] result = new byte[first.length + second.length];

        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(
            second,
            0,
            result,
            first.length,
            second.length
        );

        return result;
    }

    /**
     * Removes a specified number of bytes from the beginning of an array.
     *
     * @param source
     *     the source byte array.
     * @param count
     *     the number of bytes to remove.
     * @return
     *     the remaining bytes.
     */
    private static byte[] removePrefix(
        final byte[] source,
        final int count
    ) {
        final byte[] result = new byte[source.length - count];

        System.arraycopy(
            source,
            count,
            result,
            0,
            result.length
        );

        return result;
    }

    /**
     * Represents the current multipart parsing stage.
     */
    private enum State {

        /**
         * The parser expects the first multipart boundary.
         */
        FIRST_BOUNDARY,

        /**
         * The parser expects multipart part headers.
         */
        HEADERS,

        /**
         * The parser reads multipart part data.
         */
        DATA,

        /**
         * The parser expects a boundary suffix.
         */
        BOUNDARY_SUFFIX,

        /**
         * The final multipart boundary has been parsed.
         */
        FINISHED
    }
}
