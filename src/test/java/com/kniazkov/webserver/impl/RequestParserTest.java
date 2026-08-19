/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ContentType;
import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.ServerException;
import com.kniazkov.webserver.UploadedFile;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        assertArrayEquals(new byte[0], request.getBody());
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
            request.getBody()
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
        assertArrayEquals(bytes("Hello!"), file.getData());

        assertArrayEquals(
            bytes(body),
            request.getBody()
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

        assertArrayEquals(bytes(body), request.getBody());
        assertTrue(request.getForm().isEmpty());
        assertTrue(request.getFiles().isEmpty());
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
            firstRequest.getBody()
        );

        assertEquals(
            "/second",
            secondRequest.getPath().getPath()
        );
        assertArrayEquals(
            new byte[0],
            secondRequest.getBody()
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
        return RequestParser.parse(
            new StringByteSource(value),
            OPTIONS
        );
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
