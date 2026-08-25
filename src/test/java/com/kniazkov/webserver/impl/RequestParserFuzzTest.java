/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Bounded deterministic fuzz and property tests for {@link RequestParser}.
 * <p>
 * Every failure can be reproduced with the fixed seed and iteration number.
 * Input sizes and iteration counts are deliberately capped so this suite is
 * suitable for every CI run rather than a separate long-running fuzz job.
 */
final class RequestParserFuzzTest {

    /**
     * Seed used by every deterministic random test.
     */
    private static final long SEED = 0x20_4854_5450L;

    /**
     * Maximum generated input size.
     */
    private static final int MAX_INPUT_SIZE = 512;

    /**
     * Maximum request allocation allowed by generated cases.
     */
    private static final int MAX_REQUEST_SIZE = 2048;

    /**
     * Number of arbitrary byte inputs.
     */
    private static final int ARBITRARY_CASES = 1000;

    /**
     * Number of structured mutation inputs.
     */
    private static final int MUTATION_CASES = 1000;

    /**
     * Number of valid framing property cases.
     */
    private static final int FRAMING_CASES = 250;

    /**
     * Maximum time allowed for each bounded fuzz group.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * Parser limits used to prevent generated input from allocating
     * unbounded storage.
     */
    private static final Options OPTIONS = new Options.Builder()
        .setMaxRequestSize(MAX_REQUEST_SIZE)
        .setMaxHeaderSize(MAX_INPUT_SIZE)
        .setMaxFileSize(MAX_REQUEST_SIZE)
        .setMaxInMemoryBodySize(MAX_REQUEST_SIZE)
        .setMaxFormSize(MAX_REQUEST_SIZE)
        .setMaxMultipartParts(16)
        .setMaxMultipartHeaderSize(128)
        .build();

    /**
     * Valid request used as the starting point for structured mutations.
     */
    private static final byte[] TEMPLATE = bytes(
        "POST /submit?source=fuzz HTTP/1.1\r\n"
            + "Host: localhost\r\n"
            + "Content-Type: application/octet-stream\r\n"
            + "Content-Length: 4\r\n"
            + "Connection: close\r\n"
            + "\r\n"
            + "data"
    );

    /**
     * Verifies that arbitrary bounded bytes either produce a request or a
     * documented parser exception, never an unchecked parser failure.
     */
    @Test
    void arbitraryBytesStayInsideParserContract() {
        assertTimeoutPreemptively(
            TIMEOUT,
            () -> {
                final Random random = new Random(SEED);

                for (int index = 0; index < ARBITRARY_CASES; index++) {
                    final byte[] input = new byte[
                        random.nextInt(MAX_INPUT_SIZE + 1)
                    ];
                    random.nextBytes(input);
                    parseOrReject(input, index);
                }
            }
        );
    }

    /**
     * Verifies parser behavior for insertions, removals and replacements
     * around otherwise valid request grammar.
     */
    @Test
    void structuredMutationsStayInsideParserContract() {
        assertTimeoutPreemptively(
            TIMEOUT,
            () -> {
                final Random random = new Random(SEED ^ 0x5a5a5a5aL);

                for (int index = 0; index < MUTATION_CASES; index++) {
                    byte[] input = TEMPLATE.clone();
                    final int changes = 1 + random.nextInt(12);

                    for (int change = 0; change < changes; change++) {
                        input = mutate(input, random);
                    }

                    parseOrReject(input, index);
                }
            }
        );
    }

    /**
     * Verifies that random binary bodies are read according to their exact
     * declared length and never consume the following persistent request.
     */
    @Test
    void contentLengthPreservesNextRequestBoundary() {
        assertTimeoutPreemptively(
            TIMEOUT,
            () -> {
                final Random random = new Random(SEED ^ 0xa5a5a5a5L);

                for (int index = 0; index < FRAMING_CASES; index++) {
                    framingCase(random, index);
                }
            }
        );
    }

    /**
     * Verifies the strict decimal Content-Length grammar over a deterministic
     * set of valid and invalid generated values.
     */
    @Test
    void contentLengthUsesStrictDecimalGrammar() {
        assertTimeoutPreemptively(
            TIMEOUT,
            () -> {
                final Random random = new Random(SEED ^ 0x0f0f0f0fL);
                final String alphabet = "0123456789+-x,.a";

                for (int index = 0; index < FRAMING_CASES; index++) {
                    final int size = random.nextInt(4);
                    final StringBuilder value = new StringBuilder(size);

                    for (int offset = 0; offset < size; offset++) {
                        value.append(
                            alphabet.charAt(
                                random.nextInt(alphabet.length())
                            )
                        );
                    }

                    contentLengthCase(value.toString(), index);
                }
            }
        );
    }

    /**
     * Parses a generated input or accepts a documented rejection.
     *
     * @param input
     *     the generated input.
     * @param iteration
     *     the deterministic iteration number.
     * @throws ServerException
     *     if accepted request storage cannot be released.
     */
    private static void parseOrReject(
        final byte[] input,
        final int iteration
    ) throws ServerException {
        final Request request;

        try {
            request = RequestParser.parse(
                new ByteArrayByteSource(input),
                OPTIONS
            );
        } catch (ServerException exception) {
            return;
        } catch (RuntimeException exception) {
            throw new AssertionError(
                "Unchecked parser failure at seed "
                    + SEED
                    + ", iteration "
                    + iteration
                    + ", input "
                    + HexFormat.of().formatHex(input),
                exception
            );
        }

        close(request);
    }

    /**
     * Executes one random valid framing case.
     *
     * @param random
     *     the deterministic random source.
     * @param iteration
     *     the deterministic iteration number.
     * @throws ServerException
     *     if parsing or cleanup fails.
     */
    private static void framingCase(
        final Random random,
        final int iteration
    ) throws ServerException {
        final byte[] body = new byte[random.nextInt(129)];
        random.nextBytes(body);

        final byte[] headers = bytes(
            "POST /first HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "\r\n"
        );
        final byte[] next = bytes(
            "GET /second HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Connection: close\r\n"
                + "\r\n"
        );
        final TrackingByteSource source = new TrackingByteSource(
            concatenate(headers, body, next)
        );

        final Request first = RequestParser.parse(source, OPTIONS);
        try {
            assertArrayEquals(
                body,
                first.getBody().readAllBytes(),
                "iteration " + iteration
            );
            assertEquals(
                headers.length + body.length,
                source.getPosition(),
                "iteration " + iteration
            );
        } finally {
            close(first);
        }

        final Request second = RequestParser.parse(source, OPTIONS);
        try {
            assertEquals(
                "/second",
                second.getPath().getPath(),
                "iteration " + iteration
            );
            assertEquals(
                source.getSize(),
                source.getPosition(),
                "iteration " + iteration
            );
        } finally {
            close(second);
        }
    }

    /**
     * Executes one generated Content-Length grammar case.
     *
     * @param value
     *     the generated header value.
     * @param iteration
     *     the deterministic iteration number.
     * @throws ServerException
     *     if accepted request storage cannot be released.
     */
    private static void contentLengthCase(
        final String value,
        final int iteration
    ) throws ServerException {
        final boolean decimal = !value.isEmpty()
            && value.chars().allMatch(ch -> ch >= '0' && ch <= '9');
        final int length = decimal ? Integer.parseInt(value) : 0;
        final byte[] input = bytes(
            "POST / HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Length: " + value + "\r\n"
                + "\r\n"
                + "x".repeat(length)
        );

        if (!decimal) {
            assertThrows(
                ServerException.class,
                () -> RequestParser.parse(
                    new ByteArrayByteSource(input),
                    OPTIONS
                ),
                "iteration " + iteration + ", value " + value
            );
            return;
        }

        final Request request = RequestParser.parse(
            new ByteArrayByteSource(input),
            OPTIONS
        );
        try {
            assertEquals(
                length,
                request.getBody().getSize(),
                "iteration " + iteration + ", value " + value
            );
        } finally {
            close(request);
        }
    }

    /**
     * Applies one bounded random mutation.
     *
     * @param input
     *     the current input.
     * @param random
     *     the deterministic random source.
     * @return
     *     the mutated input.
     */
    private static byte[] mutate(
        final byte[] input,
        final Random random
    ) {
        final int operation = random.nextInt(3);

        if (operation == 0 && input.length > 0) {
            final byte[] result = input.clone();
            result[random.nextInt(result.length)] = (byte) random.nextInt();
            return result;
        }

        if (operation == 1 && input.length < MAX_INPUT_SIZE) {
            final int offset = random.nextInt(input.length + 1);
            final byte[] result = new byte[input.length + 1];
            System.arraycopy(input, 0, result, 0, offset);
            result[offset] = (byte) random.nextInt();
            System.arraycopy(
                input,
                offset,
                result,
                offset + 1,
                input.length - offset
            );
            return result;
        }

        if (input.length > 0) {
            final int offset = random.nextInt(input.length);
            final byte[] result = new byte[input.length - 1];
            System.arraycopy(input, 0, result, 0, offset);
            System.arraycopy(
                input,
                offset + 1,
                result,
                offset,
                input.length - offset - 1
            );
            return result;
        }

        return input;
    }

    /**
     * Concatenates byte arrays.
     *
     * @param values
     *     the arrays.
     * @return
     *     the combined array.
     */
    private static byte[] concatenate(final byte[]... values) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (byte[] value : values) {
            output.writeBytes(value);
        }

        return output.toByteArray();
    }

    /**
     * Closes storage owned by an accepted request.
     *
     * @param request
     *     the request.
     * @throws ServerException
     *     if cleanup fails.
     */
    private static void close(final Request request)
        throws ServerException {
        if (request instanceof ManagedRequest managed) {
            managed.close();
        }
    }

    /**
     * Converts text to US-ASCII bytes.
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
     * Byte source that exposes its current position to framing assertions.
     */
    private static final class TrackingByteSource implements ByteSource {

        /**
         * Source bytes.
         */
        private final byte[] data;

        /**
         * Current read position.
         */
        private int position;

        /**
         * Creates a tracking byte source.
         *
         * @param data
         *     the source bytes.
         */
        private TrackingByteSource(final byte[] data) {
            this.data = Arrays.copyOf(data, data.length);
        }

        /**
         * Reads one byte.
         *
         * @return
         *     the next byte, or {@code -1} at end of input.
         */
        @Override
        public int read() {
            if (position == data.length) {
                return -1;
            }
            return data[position++] & 0xff;
        }

        /**
         * Returns the current read position.
         *
         * @return
         *     the position.
         */
        private int getPosition() {
            return position;
        }

        /**
         * Returns the source size.
         *
         * @return
         *     the byte count.
         */
        private int getSize() {
            return data.length;
        }
    }
}
