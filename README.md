# Foundry19 Web Server

[![Build][build-badge]][build]
[![License: MIT][license-badge]][license]
[![Java 21][java-badge]][java]

Foundry19 Web Server is a small HTTP/HTTPS server library for Java 21. It runs
inside your application and gives each request to a plain Java handler. There
are no annotations, controllers, dependency-injection containers, servlet
APIs, or external runtime dependencies.

Version **2.0.0** is a complete redesign of the original library.

## Features

- HTTP/1.0 and HTTP/1.1 with persistent connections
- GET and POST requests
- query strings, URL-encoded forms, cookies, and multipart forms
- repeatable streaming access to request bodies and uploaded files
- bounded request, header, form, multipart, worker, and timeout resources
- static-file serving with path and symbolic-link containment
- typed response factories, status codes, and content types
- HTTPS with PKCS #12, JKS, or PEM identity material
- optional or required mutual TLS
- Java 21 virtual threads
- no external runtime dependencies

## Requirements

- Java 21 or later
- Maven 3.9 or later for building the project

## Installation

Use the following dependency for version 2.0.0:

```xml
<dependency>
    <groupId>com.kniazkov</groupId>
    <artifactId>webserver</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Quick Start

The following application listens on port `8000` and returns plain text from
one endpoint:

```java
package example;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Server;
import com.kniazkov.webserver.ServerException;

public final class Main {

    private Main() {
    }

    public static void main(final String[] args)
        throws ServerException {
        final Handler handler = (request, environment) -> {
            if (request.getPath().getPath().equals("/hello")) {
                return environment
                    .getResponseFactory()
                    .fromText("Hello, world!")
                    .build();
            }

            return environment
                .getResponseFactory()
                .notFound();
        };

        final Options options = new Options.Builder()
            .setPort(8000)
            .setHandler(handler)
            .build();

        final Server server = Server.start(options);
        System.out.println("Listening on port " + server.getPort());
    }
}
```

`Server.start()` begins accepting connections immediately. A running server
keeps the JVM alive after `main()` returns. Call `server.stop()` during your
application's shutdown sequence; it is safe to call more than once.

Port `0` asks the operating system to select a free port. The assigned port is
then available through `server.getPort()`.

## Request Handling

A `Handler` receives a parsed `Request` and an `Environment`. The environment
currently provides the `ResponseFactory` used to build responses.

### Headers and Paths

Request metadata is available without parsing raw HTTP text:

```java
final RequestHeaders headers = request.getHeaders();
final HttpMethod method = headers.getMethod();
final HttpVersion version = headers.getVersion();
final Map<String, List<String>> values = headers.getValues();

final RequestPath path = request.getPath();
final String completePath = path.getPath();
final String directory = path.getDirectory();
final String fileName = path.getFileName();
final String fileType = path.getFileType();
final ContentType contentType = path.getContentType();
```

The path is percent-decoded exactly once with strict UTF-8 and always begins
with `/`. A trailing slash is preserved. Invalid encoding, control characters,
empty interior segments, `.` and `..` segments, and encoded path separators are
rejected before the handler runs.

### Query and Form Values

Query and form values use immutable maps of immutable lists. Repeated values
are preserved:

```java
final List<String> queryValues = request
    .getQuery()
    .getOrDefault("q", List.of());

final List<String> formValues = request
    .getForm()
    .getOrDefault("tag", List.of());
```

`getForm()` contains decoded fields from URL-encoded and multipart POST bodies.
For other requests it is empty.

### Request Bodies

Every request exposes its original body as `UploadedData`:

```java
final UploadedData body = request.getBody();

try (InputStream input = body.openStream()) {
    input.transferTo(output);
}
```

`openStream()` may be called repeatedly. `transferTo(OutputStream)` streams the
body, while `readAllBytes()` deliberately loads all of it into memory. Bodies
whose declared size exceeds `maxInMemoryBodySize` use temporary-file storage.

Uploaded data remains valid only while the handler is running. Copy or process
it inside the handler; do not retain an `UploadedData` or `UploadedFile` for
later use.

### Uploaded Files

Multipart files are grouped by form field name:

```java
final List<UploadedFile> files = request
    .getFiles()
    .getOrDefault("attachment", List.of());

for (UploadedFile file : files) {
    final String name = file.getName();
    final ContentType type = file.getContentType();

    try (InputStream input = file.openStream()) {
        process(name, type, input);
    }
}
```

Small multipart data is kept in memory. Larger uploads are written to temporary
files while parsing, so the server does not need to hold the complete request in
memory. Temporary storage is released after the handler completes.

### Request Cookies

Incoming cookies are already parsed:

```java
final String session = request.getCookies().get("session");
```

## Responses

`ResponseFactory` fixes the status, content type, and body when a builder is
created. A `ResponseBuilder` can add headers and cookies, but cannot later turn
a text response into JSON or change its status.

### Factory Methods

| Method | Result |
| --- | --- |
| `fromText(String)` | `200 OK`, `text/plain`, UTF-8 |
| `fromHtml(String)` | `200 OK`, `text/html`, UTF-8 |
| `fromJson(String)` | `200 OK`, `application/json`, UTF-8 |
| `fromXml(String)` | `200 OK`, `application/xml`, UTF-8 |
| `fromBytes(byte[])` | `200 OK`, `application/octet-stream` |
| `redirect(String)` | `302 Found` with `Location` |
| `redirectPermanently(String)` | `301 Moved Permanently` with `Location` |
| `fromFile(File)` | file response, `403`, `404`, or `500` |
| `forbidden()` | `403 Forbidden` |
| `notFound()` | `404 Not Found` |
| `error(...)` | an error response |
| `noResponse()` | delegate to default static-file processing |

For example:

```java
return environment
    .getResponseFactory()
    .fromJson("{\"status\":\"ok\"}")
    .setHeader("Cache-Control", "no-store")
    .build();
```

`Content-Type`, `Content-Length`, connection, and other server-managed framing
headers cannot be supplied through a response builder. Header names and values
are validated to prevent malformed responses and header injection.

### Custom and Binary Responses

`custom(...)` is the single escape hatch for independently selecting the
status, content type, and raw body:

```java
return environment
    .getResponseFactory()
    .custom(
        HttpStatus.CREATED,
        ContentType.APPLICATION_JSON,
        "{\"id\":42}".getBytes(StandardCharsets.UTF_8)
    )
    .setHeader("Location", "/items/42")
    .build();
```

`ContentType` is an enum rather than a free-form string. Unknown file
extensions use `ContentType.APPLICATION_OCTET_STREAM`. A numeric status can be
resolved with `HttpStatus.fromCode(404)`; an unsupported number is rejected.

### Response Cookies

Simple cookies can be added directly:

```java
return environment
    .getResponseFactory()
    .fromText("Logged in")
    .setCookie("session", sessionId)
    .build();
```

Use `ResponseCookie` for attributes:

```java
final ResponseCookie cookie = new ResponseCookie.Builder(
    "session",
    sessionId
)
    .setPath("/")
    .setMaxAge(Duration.ofHours(8))
    .setSecure(true)
    .setHttpOnly(true)
    .setSameSite(SameSite.LAX)
    .build();

return environment
    .getResponseFactory()
    .fromText("Logged in")
    .setCookie(cookie)
    .build();
```

Cookie names, values, and attributes are validated when the cookie is built.
`SameSite.NONE` requires the secure attribute.

### Error Responses

`ServerException` may optionally carry an HTTP error status:

```java
throw new ServerException(
    HttpStatus.CONFLICT,
    "Resource already exists"
);
```

The server uses that status and message for the response. An exception without
a status becomes `500 Internal Server Error`, and its internal message is not
sent to the client. Parser failures use protocol-specific statuses including
`400`, `413`, `417`, `431`, `501`, and `505`.

Generated error HTML can be replaced through `Options.Builder.setErrorPage`:

```java
final ErrorPage errorPage = (code, reason, message) ->
    "<h1>Request failed</h1>";
```

`ErrorPage` controls presentation, not the response status. If a custom page
includes `reason` or `message`, it must HTML-escape them before interpolation.

## Static Files

Without a custom handler, the server serves files from the `www` directory.
With a custom handler, return `noResponse()` to use the same fallback:

```java
final Handler handler = (request, environment) -> {
    if (request.getPath().getPath().startsWith("/api/")) {
        return environment
            .getResponseFactory()
            .fromJson("{\"version\":\"2.0.0\"}")
            .build();
    }

    return environment
        .getResponseFactory()
        .noResponse();
};
```

The requested file and configured root are resolved to real filesystem paths.
A symbolic link is followed only if its target remains inside the real web
root. Missing files and links escaping that root return `404 Not Found`;
directories and unreadable files return `403 Forbidden`.

`ResponseFactory.fromFile(File)` is an explicit application response and is not
restricted to the static web root.

## HTTPS

HTTPS uses the same `Server` and `Handler`. Supply one server identity through
either a key store or PEM files.

### PKCS #12 or JKS

```java
final char[] password = loadPassword();

final SslOptions ssl = new SslOptions.Builder()
    .setKeyStoreFile("server.p12")
    .setPassword(password)
    .build();

final Server server = Server.start(
    new Options.Builder()
        .setPort(8443)
        .setSslOptions(ssl)
        .build()
);
```

PKCS #12 is the default store type. Use `setKeyStoreType(KeyStoreType.JKS)` for
JKS. If the store and private key passwords differ, use
`setKeyStorePassword(char[])` and `setKeyPassword(char[])` separately.

Password arrays are defensively copied. The builder clears the copies it owns
after `build()`; the caller remains responsible for clearing its original
array.

### PEM

```java
final SslOptions ssl = new SslOptions.Builder()
    .setCertificateChainFile("fullchain.pem")
    .setPrivateKeyFile("private-key.pk8.pem")
    .build();
```

PEM mode accepts an X.509 certificate chain and an unencrypted PKCS #8 private
key. PKCS #1 and encrypted PEM private keys are rejected; use PKCS #12 or JKS
when encrypted identity material is required.

### Protocol Policy and Mutual TLS

By default, the Java TLS provider chooses enabled protocol versions and cipher
suites. They can be restricted explicitly:

```java
final SslOptions ssl = new SslOptions.Builder()
    .setKeyStoreFile("server.p12")
    .setPassword(password)
    .setEnabledProtocols(
        SslProtocol.TLS_1_2,
        SslProtocol.TLS_1_3
    )
    .setCipherSuites("TLS_AES_128_GCM_SHA256")
    .build();
```

Mutual TLS requires explicit trust material:

```java
final SslOptions ssl = new SslOptions.Builder()
    .setKeyStoreFile("server.p12")
    .setPassword(serverPassword)
    .setTrustCertificatesFile("client-ca.pem")
    .setClientAuthentication(SslClientAuthentication.REQUIRED)
    .build();
```

A PKCS #12 or JKS trust store can be used instead. The JVM's ambient default
trust store is never used to authorize client certificates.

Run separate server instances on different ports when both HTTP and HTTPS are
required. They may share the same handler.

## Configuration

All `Options` instances are immutable. Defaults are intentionally usable for a
small server:

| Option | Default | Meaning |
| --- | ---: | --- |
| `port` | `8000` | listening port; `0` selects a free port |
| `bindAddress` | all local addresses | local interface |
| `backlog` | `50` | requested OS accept-queue length |
| `wwwRoot` | `www` | static-file root |
| `maxRequestSize` | 128 MiB | complete request limit |
| `maxFileSize` | 128 MiB | per uploaded-file limit |
| `maxInMemoryBodySize` | 64 KiB | body memory threshold |
| `maxFormSize` | 1 MiB | decoded form-data limit |
| `maxMultipartParts` | `1000` | number of multipart parts |
| `maxMultipartHeaderSize` | 16 KiB | headers of one multipart part |
| `maxHeaderSize` | 64 KiB | HTTP request line and headers |
| `maxWorkers` | `100` | concurrently processed connections |
| `readTimeout` | 30 seconds | wait for request data |
| `writeTimeout` | 30 seconds | write and flush one response |
| `handlerTimeout` | 30 seconds | handler execution |
| `handler` | static files | application behavior |
| `errorPage` | built-in HTML | generated error pages |
| `sslOptions` | disabled | HTTPS configuration |

Example:

```java
final Options options = new Options.Builder()
    .setPort(8080)
    .setBindAddress(InetAddress.getLoopbackAddress())
    .setBacklog(128)
    .setWwwRoot("public")
    .setMaxWorkers(200)
    .setMaxRequestSize(32L * 1024L * 1024L)
    .setMaxFileSize(16L * 1024L * 1024L)
    .setMaxInMemoryBodySize(64L * 1024L)
    .setMaxFormSize(1024L * 1024L)
    .setMaxMultipartParts(100)
    .setMaxMultipartHeaderSize(16L * 1024L)
    .setMaxHeaderSize(64L * 1024L)
    .setReadTimeout(Duration.ofSeconds(15))
    .setWriteTimeout(Duration.ofSeconds(15))
    .setHandlerTimeout(Duration.ofSeconds(5))
    .setHandler(handler)
    .build();
```

The worker limit applies to connections, not individual requests. A persistent
connection occupies one worker until it closes. Connections waiting for a
worker remain in the operating-system accept queue. Handler timeouts use Java
thread interruption, so long-running application code must cooperate with
interruption.

Binding to all local addresses is the default. Bind explicitly to a loopback
address when the server should be reachable only from the same host or through
a local reverse proxy.

## HTTP Behavior and Scope

- HTTP/1.1 connections are persistent unless `Connection: close` is present.
- HTTP/1.0 connections close unless `Connection: keep-alive` is accepted.
- `Content-Length` accepts decimal digits only.
- requests containing both `Content-Length` and `Transfer-Encoding` are
  rejected as ambiguous.
- unsupported transfer codings receive `501 Not Implemented`.
- `Expect: 100-continue` is supported; other expectations receive
  `417 Expectation Failed`.
- response framing and connection headers are generated by the server.
- valid but unsupported methods receive `501 Not Implemented`; malformed method
  tokens receive `400 Bad Request`.

The current public API supports HTTP/1.0 and HTTP/1.1, GET and POST, and
content-length-framed request bodies. HTTP/2, HTTP/3, WebSocket upgrades, and
chunked request bodies are outside the current scope.

## Building and Testing

Compile the project, run unit tests, build artifacts, generate Javadocs, and run
Checkstyle with:

```bash
mvn verify
```

End-to-end tests use Playwright and Chromium. Install the matching browser once
after cloning or after a Playwright upgrade:

```bash
mvn exec:java -e \
  "-Dexec.mainClass=com.microsoft.playwright.CLI" \
  "-Dexec.args=install chromium" \
  "-Dexec.classpathScope=test"
```

Then run the complete local gate, including unit and end-to-end tests:

```bash
mvn verify -Pe2e
```

CI runs that complete command for every pull request and every push to
`master`. `mvn test` runs only unit tests; it is not the complete project gate.

## Project Documentation

- [Changelog](CHANGELOG.md)
- [Code style](CODE_STYLE.md)
- [Release process](RELEASING.md)
- [Security policy](SECURITY.md)
- [MIT License](LICENSE)

[build-badge]: https://github.com/kniazkov/webserver/actions/workflows/build.yml/badge.svg
[build]: https://github.com/kniazkov/webserver/actions/workflows/build.yml
[license-badge]: https://img.shields.io/badge/License-MIT-blue.svg
[license]: LICENSE
[java-badge]: https://img.shields.io/badge/Java-21-orange.svg
[java]: https://openjdk.org/projects/jdk/21/
