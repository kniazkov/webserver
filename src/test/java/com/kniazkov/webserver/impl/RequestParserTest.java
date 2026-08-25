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
import com.kniazkov.webserver.ServerException;
import com.kniazkov.webserver.UploadedFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests integration between components used by {@link RequestParser}.
 * <p>
 * Individual parsers are tested separately. These tests verify that the
 * top-level request parser correctly combines headers, target information,
 * cookies, body data, forms and uploaded files into a complete request.
 */
final class RequestParserTest {

    /**
     * Default options used by tests.
     */
    private static final Options OPTIONS = new Options.Builder().build();

    /**
     * Tests parsing a complete GET request.
     */
    @Test
    void getRequest() throws ServerException {
        final Request request = parse(
            "GET /search/index.html?q=java&tag=http&tag=server HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Cookie: session=abc; theme=dark\r\n"
                + "\r\n"
        );

        assertEquals(HttpMethod.GET, request.getHeaders().getMethod());
        assertEquals(HttpVersion.HTTP_1_1, request.getHeaders().getVersion());

        assertEquals(
            "/search/index.html?q=java&tag=http&tag=server",
            request.getHeaders().getTarget()
        );

        assertEquals("/search/index.html", request.getPath().getPath());
        assertEquals("/search/", request.getPath().getDirectory());
        assertEquals("index.html", request.getPath().getFileName());
        assertEquals("html", request.getPath().getFileType());
        assertEquals(
            ContentType.TEXT_HTML,
            request.getPath().getContentType()
        );

        assertEquals(
            Map.of(
                "q", List.of("java"),
                "tag", List.of("http", "server")
            ),
            request.getQuery()
        );

        assertEquals(
            Map.of(
                "session", "abc",
                "theme", "dark"
            ),
            request.getCookies()
        );

        assertTrue(request.getForm().isEmpty());
        assertTrue(request.getFiles().isEmpty());
        assertArrayEquals(new byte[0], request.getBody().readAllBytes());
    }

    /**
     * Tests that path and query components use their respective decoding
     * rules.
     */
    @Test
    void encodedPathAndQuery() throws ServerException {
        final Request request = parse(
            "GET /docs/My%20File+%3F.txt?term=a+b HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "\r\n"
        );

        assertEquals(
            "/docs/My File+?.txt",
            request.getPath().getPath()
        );
        assertEquals(
            Map.of("term", List.of("a b")),
            request.getQuery()
        );
        assertEquals(
            "/docs/My%20File+%3F.txt?term=a+b",
            request.getHeaders().getTarget()
        );
    }

    /**
     * Tests parsing a URL-encoded POST request.
     */
    @Test
    void urlEncodedPost() throws ServerException {
        final String body =
            "name=Ivan&language=Java&language=HTTP";

        final Request request = parse(
            "POST /submit?source=test HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: " + bytes(body).length + "\r\n"
                + "\r\n"
                + body
        );

        assertEquals(
            Map.of(
                "source", List.of("test")
            ),
            request.getQuery()
        );

        assertEquals(
            Map.of(
                "name", List.of("Ivan"),
                "language", List.of("Java", "HTTP")
            ),
            request.getForm()
        );

        assertArrayEquals(
            bytes(body),
            request.getBody().readAllBytes()
        );

        assertTrue(request.getFiles().isEmpty());
    }

    /**
     * Tests parsing a multipart POST request.
     */
    @Test
    void multipartPost() throws ServerException {
        final String boundary = "test-boundary";

        final String body =
            "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"\r\n"
                + "\r\n"
                + "Example\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"hello.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + "Hello!\r\n"
                + "--" + boundary + "--";

        final Request request = parse(
            "POST /upload HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: multipart/form-data; "
                + "boundary=" + boundary + "\r\n"
                + "Content-Length: " + bytes(body).length + "\r\n"
                + "\r\n"
                + body
        );

        assertEquals(
            Map.of(
                "title", List.of("Example")
            ),
            request.getForm()
        );

        final UploadedFile file =
            request.getFiles().get("file").getFirst();

        assertEquals("hello.txt", file.getName());
        assertEquals(ContentType.TEXT_PLAIN, file.getContentType());
        assertArrayEquals(bytes("Hello!"), file.readAllBytes());

        assertArrayEquals(
            bytes(body),
            request.getBody().readAllBytes()
        );
    }

    /**
     * Tests a POST body with an unknown content type.
     */
    @Test
    void rawBody() throws ServerException {
        final String body = "arbitrary raw data";

        final Request request = parse(
            "POST /data HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "Content-Length: " + bytes(body).length + "\r\n"
                + "\r\n"
                + body
        );

        assertArrayEquals(bytes(body), request.getBody().readAllBytes());
        assertTrue(request.getForm().isEmpty());
        assertTrue(request.getFiles().isEmpty());
    }

    /**
     * Tests that a body larger than the in-memory threshold uses temporary
     * storage and is released together with the request.
     */
    @Test
    void temporaryBodyStorageLifecycle() throws ServerException {
        final String body = "stored outside the heap";
        final Options options = new Options.Builder()
            .setMaxInMemoryBodySize(0)
            .build();

        final Request request = parse(
            "POST /data HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Length: " + bytes(body).length + "\r\n"
                + "\r\n"
                + body,
            options
        );

        assertInstanceOf(
            TemporaryFileUploadedData.class,
            request.getBody()
        );
        assertArrayEquals(
            bytes(body),
            request.getBody().readAllBytes()
        );

        assertInstanceOf(ManagedRequest.class, request).close();

        assertThrows(
            ServerException.class,
            request.getBody()::openStream
        );
    }

    /**
     * Tests that uploaded files are bounded views of the stored request body.
     */
    @Test
    void multipartFileUsesRequestStorage() throws ServerException {
        final String boundary = "storage-boundary";
        final String body =
            "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\"data.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "\r\n"
                + "file bytes\r\n"
                + "--" + boundary + "--";

        final Options options = new Options.Builder()
            .setMaxInMemoryBodySize(0)
            .build();

        final Request request = parse(
            "POST /upload HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: multipart/form-data; boundary="
                + boundary + "\r\n"
                + "Content-Length: " + bytes(body).length + "\r\n"
                + "\r\n"
                + body,
            options
        );

        final UploadedFile file = request
            .getFiles()
            .get("file")
            .getFirst();

        assertArrayEquals(bytes("file bytes"), file.readAllBytes());

        assertInstanceOf(ManagedRequest.class, request).close();

        assertThrows(ServerException.class, file::openStream);
    }

    /**
     * Tests the independent decoded form data limit.
     */
    @Test
    void formSizeLimit() {
        final String body = "field=too-long";
        final Options options = new Options.Builder()
            .setMaxFormSize(4)
            .build();

        assertThrows(
            ServerException.class,
            () -> parse(
                "POST /submit HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Type: "
                    + "application/x-www-form-urlencoded\r\n"
                    + "Content-Length: " + bytes(body).length + "\r\n"
                    + "\r\n"
                    + body,
                options
            )
        );
    }

    /**
     * Tests that HTTP/1.1 requires a Host header.
     */
    @Test
    void http11WithoutHost() {
        assertThrows(
            ServerException.class,
            () -> parse(
                "GET / HTTP/1.1\r\n"
                    + "\r\n"
            )
        );
    }

    /**
     * Tests that HTTP/1.0 does not require a Host header.
     */
    @Test
    void http10WithoutHost() throws ServerException {
        final Request request = parse(
            "GET / HTTP/1.0\r\n"
                + "\r\n"
        );

        assertEquals(
            HttpVersion.HTTP_1_0,
            request.getHeaders().getVersion()
        );

        assertEquals("/", request.getPath().getPath());
    }

    /**
     * Tests that HTTP/1.1 contains exactly one Host header.
     */
    @Test
    void duplicateHost() {
        assertThrows(
            ServerException.class,
            () -> parse(
                "GET / HTTP/1.1\r\n"
                    + "Host: one.example\r\n"
                    + "Host: two.example\r\n"
                    + "\r\n"
            )
        );
    }

    /**
     * Tests rejection of unsupported transfer encoding.
     */
    @Test
    void transferEncoding() {
        assertThrows(
            ServerException.class,
            () -> parse(
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Transfer-Encoding: chunked\r\n"
                    + "\r\n"
                    + "0\r\n\r\n"
            )
        );
    }

    /**
     * Tests rejection of several Content-Length headers.
     */
    @Test
    void duplicateContentLength() {
        assertThrows(
            ServerException.class,
            () -> parse(
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: 3\r\n"
                    + "Content-Length: 3\r\n"
                    + "\r\n"
                    + "abc"
            )
        );
    }

    /**
     * Tests rejection of an invalid Content-Length value.
     */
    @Test
    void invalidContentLength() {
        assertThrows(
            ServerException.class,
            () -> parse(
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: banana\r\n"
                    + "\r\n"
            )
        );
    }

    /**
     * Tests that Content-Length follows the strict decimal grammar rather
     * than the more permissive Java signed-integer grammar.
     */
    @Test
    void nonDecimalContentLength() {
        final List<String> invalid = List.of(
            "",
            "+5",
            "-0",
            "5.0",
            "0x5",
            "5, 5",
            "9223372036854775808"
        );

        for (String value : invalid) {
            assertThrows(
                ServerException.class,
                () -> parse(
                    "POST / HTTP/1.1\r\n"
                        + "Host: localhost\r\n"
                        + "Content-Length: " + value + "\r\n"
                        + "\r\n"
                ),
                value
            );
        }
    }

    /**
     * Tests rejection of ambiguous request framing used by request-smuggling
     * attacks.
     */
    @Test
    void contentLengthWithTransferEncoding() {
        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parse(
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: 4\r\n"
                    + "Transfer-Encoding: chunked\r\n"
                    + "\r\n"
                    + "0\r\n\r\n"
            )
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatus().orElseThrow()
        );
    }

    /**
     * Tests requesting an interim response before sending a request body.
     */
    @Test
    void expectContinue() throws Exception {
        final AtomicBoolean continued = new AtomicBoolean();

        final Request request = RequestParser.parse(
            new StringByteSource(
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: 5\r\n"
                    + "Expect: 100-Continue\r\n"
                    + "\r\n"
                    + "hello"
            ),
            OPTIONS,
            () -> continued.set(true)
        );

        assertTrue(continued.get());
        assertArrayEquals(
            bytes("hello"),
            request.getBody().readAllBytes()
        );
    }

    /**
     * Tests that a rejected request does not receive an interim response that
     * would invite the client to transmit an oversized body.
     */
    @Test
    void oversizedExpectationIsNotContinued() {
        final AtomicBoolean continued = new AtomicBoolean();
        final Options options = new Options.Builder()
            .setMaxHeaderSize(256)
            .setMaxRequestSize(128)
            .build();

        final ServerException exception = assertThrows(
            ServerException.class,
            () -> RequestParser.parse(
                new StringByteSource(
                    "POST / HTTP/1.1\r\n"
                        + "Host: localhost\r\n"
                        + "Content-Length: 1024\r\n"
                        + "Expect: 100-continue\r\n"
                        + "\r\n"
                ),
                options,
                () -> continued.set(true)
            )
        );

        assertEquals(
            HttpStatus.PAYLOAD_TOO_LARGE,
            exception.getStatus().orElseThrow()
        );
        assertFalse(continued.get());
    }

    /**
     * Tests rejection of an unsupported request expectation.
     */
    @Test
    void unsupportedExpectation() {
        final ServerException exception = assertThrows(
            ServerException.class,
            () -> parse(
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Expect: diagnostic-feature\r\n"
                    + "\r\n"
            )
        );

        assertEquals(
            HttpStatus.EXPECTATION_FAILED,
            exception.getStatus().orElseThrow()
        );
    }

    /**
     * Tests rejection of a body shorter than Content-Length.
     */
    @Test
    void incompleteBody() {
        assertThrows(
            ServerException.class,
            () -> parse(
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: 10\r\n"
                    + "\r\n"
                    + "abc"
            )
        );
    }

    /**
     * Tests rejection of several Content-Type headers.
     */
    @Test
    void duplicateContentType() {
        assertThrows(
            ServerException.class,
            () -> parse(
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Type: text/plain\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: 3\r\n"
                    + "\r\n"
                    + "abc"
            )
        );
    }

    /**
     * Tests that parsing one request does not consume the following request.
     */
    @Test
    void nextRequestRemainsUnread() throws ServerException {
        final String first =
            "POST /first HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Length: 5\r\n"
                + "\r\n"
                + "hello";

        final String second =
            "GET /second HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "\r\n";

        final ByteSource source =
            new StringByteSource(first + second);

        final Request firstRequest =
            RequestParser.parse(source, OPTIONS);

        final Request secondRequest =
            RequestParser.parse(source, OPTIONS);

        assertEquals(
            "/first",
            firstRequest.getPath().getPath()
        );
        assertArrayEquals(
            bytes("hello"),
            firstRequest.getBody().readAllBytes()
        );

        assertEquals(
            "/second",
            secondRequest.getPath().getPath()
        );
        assertArrayEquals(
            new byte[0],
            secondRequest.getBody().readAllBytes()
        );
    }

    /**
     * Tests the maximum request size across headers and body together.
     */
    @Test
    void requestSizeLimit() {
        final String request =
            "POST / HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Length: 5\r\n"
                + "\r\n"
                + "hello";

        final byte[] requestBytes = bytes(request);
        final Options options = new Options.Builder()
            .setMaxHeaderSize(requestBytes.length - 5)
            .setMaxFileSize(0)
            .setMaxRequestSize(requestBytes.length - 1)
            .build();

        assertThrows(
            ServerException.class,
            () -> RequestParser.parse(
                new StringByteSource(request),
                options
            )
        );
    }

    /**
     * Tests that an exhausted source before a new request is reported as a
     * normally closed connection.
     */
    @Test
    void connectionClosedBeforeRequest() {
        assertThrows(
            ConnectionClosedException.class,
            () -> RequestParser.parse(
                new StringByteSource(""),
                OPTIONS
            )
        );
    }

    /**
     * Tests that a connection closed in the middle of a request is reported as
     * an invalid request rather than a normally closed connection.
     */
    @Test
    void connectionClosedDuringRequest() {
        final ServerException exception = assertThrows(
            ServerException.class,
            () -> RequestParser.parse(
                new StringByteSource("GET / HTTP/1.1\r\nHost: local"),
                OPTIONS
            )
        );

        assertFalse(exception instanceof ConnectionClosedException);
    }

    /**
     * Tests the public status and message assigned to request parsing errors.
     * Diagnostic details must remain available only as the exception cause.
     */
    @Test
    void clientErrorMapping() {
        assertClientError(
            HttpStatus.BAD_REQUEST,
            "Invalid HTTP header: X-Diagnostic-Marker",
            () -> parse(
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "X-Diagnostic-Marker\r\n"
                    + "\r\n"
            )
        );

        assertClientError(
            HttpStatus.NOT_IMPLEMENTED,
            "Unsupported HTTP method: DIAGNOSTIC-METHOD",
            () -> parse(
                "DIAGNOSTIC-METHOD / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "\r\n"
            )
        );

        assertClientError(
            HttpStatus.NOT_IMPLEMENTED,
            "Unsupported HTTP method: get",
            () -> parse(
                "get / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "\r\n"
            )
        );

        assertClientError(
            HttpStatus.BAD_REQUEST,
            "Invalid HTTP method: G@T",
            () -> parse(
                "G@T / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "\r\n"
            )
        );

        assertClientError(
            HttpStatus.HTTP_VERSION_NOT_SUPPORTED,
            "Unsupported HTTP version: HTTP/9.9-DIAGNOSTIC",
            () -> parse(
                "GET / HTTP/9.9-DIAGNOSTIC\r\n"
                    + "Host: localhost\r\n"
                    + "\r\n"
            )
        );

        assertClientError(
            HttpStatus.NOT_IMPLEMENTED,
            "Transfer-Encoding is not supported",
            () -> parse(
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Transfer-Encoding: chunked\r\n"
                    + "\r\n"
            )
        );

        assertClientError(
            HttpStatus.EXPECTATION_FAILED,
            "Unsupported request expectation",
            () -> parse(
                "GET / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Expect: diagnostic-feature\r\n"
                    + "\r\n"
            )
        );

        final Options requestLimit = new Options.Builder()
            .setMaxRequestSize(64)
            .setMaxHeaderSize(64)
            .build();

        assertClientError(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "Maximum HTTP request size exceeded",
            () -> parse(
                "POST / HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: 100\r\n"
                    + "\r\n",
                requestLimit
            )
        );

        final Options headerLimit = new Options.Builder()
            .setMaxRequestSize(128)
            .setMaxHeaderSize(32)
            .build();

        assertClientError(
            HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE,
            "Maximum HTTP header size exceeded",
            () -> parse(
                "GET / HTTP/1.1\r\n"
                    + "Host: diagnostic.example\r\n"
                    + "\r\n",
                headerLimit
            )
        );
    }

    /**
     * Parses an HTTP request.
     *
     * @param value
     *     the complete HTTP request.
     * @return
     *     the parsed request.
     * @throws ServerException
     *     if parsing fails.
     */
    private static Request parse(final String value)
        throws ServerException {
        return parse(value, OPTIONS);
    }

    /**
     * Parses an HTTP request using explicit options.
     *
     * @param value
     *     the complete HTTP request.
     * @param options
     *     the server options.
     * @return
     *     the parsed request.
     * @throws ServerException
     *     if parsing fails.
     */
    private static Request parse(
        final String value,
        final Options options
    ) throws ServerException {
        return RequestParser.parse(
            new StringByteSource(value),
            options
        );
    }

    /**
     * Verifies one parser error exposed to an HTTP client.
     *
     * @param status
     *     the expected response status.
     * @param diagnostic
     *     the expected server-side diagnostic message.
     * @param action
     *     the parsing action.
     */
    private static void assertClientError(
        final HttpStatus status,
        final String diagnostic,
        final Executable action
    ) {
        final ServerException exception = assertThrows(
            ServerException.class,
            action
        );

        assertEquals(status, exception.getStatus().orElseThrow());
        assertEquals(status.getReason(), exception.getMessage());
        assertInstanceOf(ServerException.class, exception.getCause());
        assertEquals(diagnostic, exception.getCause().getMessage());
    }

    /**
     * Converts text to ASCII bytes.
     *
     * @param value
     *     the text.
     * @return
     *     the bytes.
     */
    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
