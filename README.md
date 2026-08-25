# Foundry19 Web Server

![Build](https://github.com/kniazkov/webserver/actions/workflows/build.yml/badge.svg)

Foundry19 Web Server is a lightweight HTTP/HTTPS server library for Java 21, designed around a simple idea: running a web server should not require adopting a framework, learning a large configuration system, or surrendering control of your application to somebody else's architecture.

> “Do one thing and do it well.”  
> — Unix philosophy

The server provides a small, straightforward API for receiving HTTP requests and producing HTTP responses. It handles the protocol-level work such as request parsing, forms, file uploads, cookies, persistent connections, static files, limits, and HTTPS, while leaving application logic entirely in your hands.

There are no controllers, annotations, dependency injection containers, or application lifecycle rituals. A handler receives a `Request`, returns a `Response`, and that is essentially the contract.

A minimal server requires only a few lines:

```java
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Server;

public final class Main {

    public static void main(final String[] args) throws Exception {
        final Server server = Server.start(
            new Options.Builder().build()
        );
    }
}
```

By default, the server can serve static files from its configured web root. Custom request handling is just as small:

```java
final Options options = new Options.Builder()
    .setHandler(
        (request, environment) ->
            environment
                .getResponseFactory()
                .fromText("Hello, world!")
                .build()
    )
    .build();

final Server server = Server.start(options);
```

That is the basic model throughout the library: configure what you need, implement the behavior you need, and let the server deal with HTTP. The public API is intentionally kept small and predictable so that simple applications stay simple and more complicated applications can be built without fighting the library.

## Requirements

Foundry19 Web Server requires:

- **Java 21** or later
- **Maven** for dependency management and building

The library has no external runtime requirements. The server runs directly inside your Java application and does not require a separate application server, servlet container, or additional deployment environment.

## Installation

Foundry19 Web Server is available as a Maven dependency.

Add the following to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>com.kniazkov</groupId>
        <artifactId>webserver</artifactId>
        <version>2.0.0</version>
    </dependency>
</dependencies>
```

Once the dependency is added, the server becomes part of your application. There is nothing else to install or deploy separately.

Version **2.0.0** is the second generation of the library, with a redesigned API and server architecture focused on simplicity, predictable behavior, and modern Java.

### No Dependency Bloat

Adding Foundry19 Web Server does not pull a small civilization of third-party libraries into your project. The server is built on the Java standard library and has **no external runtime dependencies**, keeping your dependency tree small, predictable, and under your control.

## Starting the Server

Starting a server requires only an `Options` instance:

```java
final Server server = Server.start(
    new Options.Builder().build()
);
````

The server starts listening immediately using the configured options. When the application no longer needs it, stop it explicitly:

```java
server.stop();
```

With the default configuration, requests that are not handled explicitly are resolved as static files from the configured web root. This makes it possible to run a useful server without writing a handler at all.

Configuration is performed through `Options.Builder`, so the basic startup code remains the same as features are added:

```java
final Options options = new Options.Builder()
    .setPort(8080)
    .setBindAddress(InetAddress.getLoopbackAddress())
    .setBacklog(128)
    .setWwwRoot("public")
    .build();

final Server server = Server.start(options);
```

There is no separate bootstrap process, container, configuration file, or framework lifecycle. `Server.start()` starts the server; `Server.stop()` stops it. Apparently this can, in fact, be that simple.

## Handling Requests

Application-specific behavior is implemented through the `Handler` interface. A handler receives the parsed `Request` together with an `Environment` and returns a `Response`:

```java
final Handler handler = (request, environment) ->
    environment
        .getResponseFactory()
        .fromText("Hello, world!")
        .build();
```

The handler is attached to the server through `Options`:

```java
final Options options = new Options.Builder()
    .setPort(8080)
    .setHandler(handler)
    .build();

final Server server = Server.start(options);
```

The `Request` contains the information extracted from the HTTP request, while the `Environment` provides server services intended for use by application code. In particular, it gives access to the `ResponseFactory`, which is the standard way to construct responses.

A handler can inspect the request and choose a response using ordinary Java code:

```java
final Handler handler = (request, environment) -> {
    final ResponseFactory factory =
        environment.getResponseFactory();

    if (request.getPath().getFullPath().equals("/hello")) {
        return factory
            .fromText("Hello!")
            .build();
    }

    return factory.notFound();
};
```

A handler may also return:

```java
return environment
    .getResponseFactory()
    .noResponse();
```

`noResponse()` tells the server that the handler does not want to produce a response itself. The server then falls back to its default behavior, such as looking for a static file corresponding to the requested path.

This allows custom logic and static content to coexist without requiring the application to reimplement file serving:

```java
final Handler handler = (request, environment) -> {
    if (request.getPath().getFullPath().equals("/api/status")) {
        return environment
            .getResponseFactory()
            .fromJson("{\"status\":\"ok\"}")
            .build();
    }

    return environment
        .getResponseFactory()
        .noResponse();
};
```

Here `/api/status` is handled by application code, while other requests are left to the server's default processing.

## Working with Requests

The `Request` object contains the HTTP request after it has been parsed by the server. Application code does not need to manually split URLs, decode form parameters, parse cookies, or interpret multipart data.

The request path is available separately from query parameters:

```java
final RequestPath path = request.getPath();

final String fullPath = path.getPath();
final String directory = path.getDirectory();
final String fileName = path.getFileName();
final String extension = path.getFileType();
final ContentType contentType = path.getContentType();
````

For example, a request for:

```text
/images/photo.jpg?size=large
```

provides the path information independently from the query string. File extensions and their corresponding `ContentType` values are determined by the server.

### Request Path Normalization

The request path is decoded into one canonical application path before it is
exposed through `RequestPath` or used for static-file lookup. The processing
order is deliberately strict:

1. The raw query string is separated from the raw path.
2. Literal `/` characters divide the path into segments.
3. Each segment is percent-decoded exactly once using strict UTF-8.
4. The decoded segments are validated before the path is used.

For example, `/documents/My%20File.txt` is exposed to a handler as
`/documents/My File.txt`. A `+` in a path remains a literal plus; only query and
form data use the convention that decodes `+` as a space.

A trailing slash is preserved. Consequently, `/documents/` has the directory
`/documents/` and an empty file name instead of being rejected or silently
changed into `/documents`.

Malformed percent encoding, invalid UTF-8, control characters, empty interior
segments, and `.` or `..` segments are rejected with `400 Bad Request`.
Percent-encoded `/` and `\` characters are also rejected rather than being
allowed to change the path structure. Decoding happens only once, so a value
such as `%252e%252e` becomes the literal segment `%2e%2e`, never a parent
directory traversal.

### Query Parameters

Query parameters are parsed into collections of values, so repeated parameters are preserved:

```text
/search?q=java&q=http&page=2
```

They can be accessed directly from the request:

```java
final Map<String, List<String>> query = request.getQuery();

final List<String> terms = query.get("q");
final String page = query.get("page").getFirst();
```

In this example, `terms` contains both `java` and `http`.

### Form Data

URL-encoded POST forms are parsed in the same way:

```java
final Map<String, List<String>> form = request.getForm();

final String name = form.get("name").getFirst();
```

This means query parameters and form fields follow the same basic data model, including fields that occur more than once.

### Request Body

The complete request body is always available as `UploadedData`:

```java
final UploadedData body = request.getBody();

try (InputStream input = body.openStream()) {
    // Process the body without allocating one array for all of it.
}
```

`openStream()` may be called more than once. `readAllBytes()` is available for
small bodies when deliberately loading the complete body into memory is more
convenient.

### Uploaded Files

Files submitted using `multipart/form-data` are parsed separately and exposed through the request:

```java
final Map<String, List<UploadedFile>> files =
    request.getFiles();

final UploadedFile file =
    files.get("avatar").getFirst();
```

`UploadedFile` extends `UploadedData`, adding the original file name and content
type. The same streaming and explicit `readAllBytes()` operations are therefore
available for request bodies and multipart files.

### Cookies

Cookies received from the client are parsed from the HTTP headers and made directly available through the request:

```java
final Map<String, String> cookies =
    request.getCookies();

final String session = cookies.get("session");
```

Application code therefore normally works with structured request data rather than raw HTTP syntax. The raw protocol parsing remains where it belongs: somewhere else.

## Creating Responses

Responses are created through the `ResponseFactory` available from the handler's `Environment`:

```java
final ResponseFactory factory =
    environment.getResponseFactory();
```

The factory provides common responses directly and builders for responses that may require additional headers or cookies.

### Text Responses

A plain text response can be created with:

```java
return factory
    .fromText("Hello, world!")
    .build();
```

HTML and JSON responses work the same way:

```java
return factory
    .fromHtml("<h1>Hello!</h1>")
    .build();
```

```java
return factory
    .fromJson("{\"status\":\"ok\"}")
    .build();
```

The appropriate `Content-Type` is selected automatically. Text created by
these factory methods is encoded as UTF-8 and the response declares
`charset=UTF-8` explicitly.

### Status, Binary Data, and Content Types

Factory methods fix the status, content type, and body when the builder is
created. The builder can add headers and cookies, but cannot turn a text
response into JSON or change its status.

When no specific factory method fits, one explicit escape hatch accepts all
three fundamental response properties:

```java
return factory
    .custom(
        HttpStatus.CREATED,
        ContentType.APPLICATION_JSON,
        "{\"id\":42}".getBytes(StandardCharsets.UTF_8)
    )
    .setHeader("Location", "/items/42")
    .build();
```

Content types are selected through the `ContentType` enum. The enum covers
common web documents, structured data, archives, office documents, fonts,
images, audio, video, and 3D models, so misspelled media types cannot enter an
outgoing response.

Raw bytes can be returned without text conversion:

```java
return factory
    .fromBytes(packet)
    .build();
```

`fromBytes(byte[])` is always `200 OK` with
`application/octet-stream`. Custom status or media-type combinations must use
`custom(...)`, making the unsafe path deliberate and visible at the call site.

### Headers

Additional response headers can be added before building the response:

```java
return factory
    .fromText("Hello")
    .setHeader("X-Application", "example")
    .build();
```

Framing, representation, and hop-by-hop headers managed by the HTTP server
cannot be supplied through the builder. The reserved names are `Close`,
`Connection`, `Content-Length`, `Content-Type`, `Keep-Alive`,
`Proxy-Connection`, `TE`, `Trailer`, `Transfer-Encoding`, and `Upgrade`.
The final content length and connection policy are generated from the actual
response and request. Header values containing unsafe control characters are
rejected; horizontal tab remains available where HTTP field syntax permits it.

### Cookies

Cookies are handled separately from ordinary headers:

```java
return factory
    .fromText("Logged in")
    .setCookie("session", "abc123")
    .build();
```

This keeps cookie handling explicit without requiring application code to manually construct `Set-Cookie` headers.

Cookie attributes are represented by `ResponseCookie`:

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

return factory
    .fromText("Logged in")
    .setCookie(cookie)
    .build();
```

`ResponseCookie` validates itself when built and its `toString()` method
returns the complete `Set-Cookie` header value.

### Standard Responses

Common HTTP responses are available directly from the factory:

```java
return factory.notFound();
```

Numeric status codes can be converted to their enum value when needed:

```java
final HttpStatus status = HttpStatus.fromCode(404);
```

For an internal server error:

```java
return factory.error();
```

or, when an error is represented by a `ServerException`:

```java
return factory.error(exception);
```

An exception without a status is treated as an internal error. Its message is
not exposed to the client. A handler can deliberately return an HTTP error by
including a status:

```java
throw new ServerException(
    HttpStatus.CONFLICT,
    "Resource already exists"
);
```

The server then returns `409 Conflict` and passes the supplied message to the
configured error page. Request parsing errors similarly use protocol-specific
statuses such as `400`, `413`, `431`, `501`, and `505`.

The appearance of generated error pages can be customized through the server configuration without changing handler code.

### File Responses

A file can be returned directly:

```java
return factory.fromFile(file);
```

The server reads the file as binary data and determines its `Content-Type` from the file extension.

If the file does not exist, the factory returns a `404 Not Found` response. If the path refers to a directory or the file cannot be accessed, it returns `403 Forbidden`.

### Default Processing

A handler does not have to produce a response for every request. It can explicitly delegate processing back to the server:

```java
return factory.noResponse();
```

This is particularly useful when application endpoints and static files share the same server:

```java
if (request.getPath().getFullPath().equals("/api/version")) {
    return factory
        .fromJson("{\"version\":\"2.0\"}")
        .build();
}

return factory.noResponse();
```

The application handles `/api/version`; everything else is left to the server's default processing.

Static-file fallback resolves both the configured `wwwRoot` and the requested
file to their real filesystem paths. Symbolic links are followed only when
their final target remains inside the real `wwwRoot`; a dangling link or a
link that escapes the root produces `404 Not Found`. This containment policy
does not restrict files deliberately returned by application code through
`ResponseFactory.fromFile`.

The result is deliberately uncomplicated: inspect a `Request`, use a `ResponseFactory`, return a `Response`. No annotations, reflection tricks, controller hierarchies, or other ceremonial machinery are required.

## Uploading Files

File uploads using `multipart/form-data` are parsed automatically by the server. Uploaded files are separated from ordinary form fields and exposed through the `Request` API.

For example, given an HTML form:

```html
<form method="post" enctype="multipart/form-data">
    <input type="text" name="description">
    <input type="file" name="image">
    <button type="submit">Upload</button>
</form>
````

the text field is available as ordinary form data:

```java
final String description =
    request.getForm()
        .get("description")
        .getFirst();
```

while the uploaded file is available separately:

```java
final UploadedFile image =
    request.getFiles()
        .get("image")
        .getFirst();
```

Uploaded files contain the information already extracted from the multipart request, including the original file name, content type, and binary data. Application code therefore does not need to parse multipart boundaries, disposition headers, or raw request bodies itself.

An uploaded file can be copied without loading it completely into memory:

```java
try (OutputStream output = Files.newOutputStream(destination)) {
    image.transferTo(output);
}
```

Multiple files submitted under the same field name are preserved:

```java
final List<UploadedFile> images =
    request.getFiles().get("images");
```

The server treats uploaded data as binary data. If the client does not provide a recognized content type, the file is still accepted as binary content rather than being rejected merely because its type is unknown.

File uploads are subject to the configured maximum file size. This limit is
applied to each uploaded file independently.

The overall request size is controlled separately by the request size limit.
Small request bodies are kept in memory. If the declared `Content-Length`
exceeds `maxInMemoryBodySize`, the server streams the body into one temporary
file. Multipart files then become bounded views of that storage instead of
additional copies. Temporary storage and all opened streams are released after
the handler finishes, so uploaded data must be consumed or copied during the
handler call.

Multipart metadata is bounded independently from file and form contents.
`maxMultipartParts` limits the number of fields and files in one request, while
`maxMultipartHeaderSize` limits the header section of each part. Boundary
values are validated against the RFC 2046 grammar and its 70-character limit.

## Working with Cookies

Cookies received from the client are parsed automatically and exposed through the `Request` object:

```java
final Map<String, String> cookies =
    request.getCookies();
```

A particular cookie can then be accessed directly:

```java
final String session =
    request.getCookies().get("session");
```

There is no need to locate the `Cookie` header or parse its contents manually.

Cookies can also be added to responses through `ResponseBuilder`:

```java
return environment
    .getResponseFactory()
    .fromText("Welcome back")
    .setCookie("session", "abc123")
    .build();
```

This produces the corresponding `Set-Cookie` response header while keeping cookie handling separate from ordinary response headers.

Cookies can therefore be read and written using the same request-response model as the rest of the library:

```java
final String session =
    request.getCookies().get("session");

if (session == null) {
    return environment
        .getResponseFactory()
        .fromText("New session")
        .setCookie("session", createSessionId())
        .build();
}

return environment
    .getResponseFactory()
    .fromText("Existing session")
    .build();
```

The server handles the HTTP syntax; application code deals with cookie names and values. Raw `Cookie` and `Set-Cookie` headers remain available when needed, but routine cookie handling does not require constructing or parsing them by hand.

## HTTPS

HTTPS can be enabled by providing an `SslOptions` instance in the server configuration. The rest of the application does not need to change: handlers, requests, responses, static files, and other server features work the same way over HTTP and HTTPS.

A typical configuration using a PKCS #12 key store looks like this:

```java
final SslOptions sslOptions = new SslOptions.Builder()
    .setKeyStoreFile("certificate.p12")
    .setPassword("changeit")
    .build();

final Options options = new Options.Builder()
    .setPort(8443)
    .setSslOptions(sslOptions)
    .build();

final Server server = Server.start(options);
````

`PKCS12` is the default key store type, and `TLS` is the default protocol, so they do not normally need to be specified explicitly.

If necessary, they can be selected explicitly:

```java
final SslOptions sslOptions = new SslOptions.Builder()
    .setKeyStoreFile("certificate.p12")
    .setPassword("changeit")
    .setKeyStoreType(KeyStoreType.PKCS12)
    .setProtocol(SslProtocol.TLS)
    .build();
```

JKS key stores are also supported:

```java
final SslOptions sslOptions = new SslOptions.Builder()
    .setKeyStoreFile("server.jks")
    .setPassword("changeit")
    .setKeyStoreType(KeyStoreType.JKS)
    .build();
```

If the private key and the key store use different passwords, they can be configured separately:

```java
final SslOptions sslOptions = new SslOptions.Builder()
    .setKeyStoreFile("certificate.p12")
    .setKeyStorePassword("store-password")
    .setKeyPassword("key-password")
    .build();
```

`SslOptions.Builder.build()` verifies that the specified key store exists, is a regular file, and can be read. Problems with the actual key store contents, password, certificate, or TLS initialization are detected when the server starts.

If no `SslOptions` are supplied, the server uses ordinary HTTP. There is no separate HTTPS server API and no `sslEnabled` flag to keep synchronized with the actual configuration.

## Server Options

Server behavior is configured through `Options.Builder`. All settings have defaults, so applications only need to specify values they actually want to change.

A more extensively configured server might look like this:

```java
final Options options = new Options.Builder()
    .setPort(8080)
    .setWwwRoot("public")
    .setMaxWorkers(200)
    .setReadTimeout(Duration.ofSeconds(15))
    .setWriteTimeout(Duration.ofSeconds(15))
    .setHandlerTimeout(Duration.ofSeconds(10))
    .setHandler(handler)
    .setErrorPage(errorPage)
    .build();
```

The main configuration options include:

| Option                    | Purpose                                                     |
| ------------------------- | ----------------------------------------------------------- |
| `port`                    | TCP port on which the server listens                        |
| `bindAddress`             | Local address on which the server listens                   |
| `backlog`                 | Requested operating-system accept queue size                |
| `wwwRoot`                 | Root directory used for static files                        |
| `maxRequestSize`          | Maximum size of a complete HTTP request                     |
| `maxFileSize`             | Maximum size of an individual uploaded file                 |
| `maxInMemoryBodySize`     | Largest declared body stored in memory                      |
| `maxFormSize`             | Maximum decoded non-file form data                          |
| `maxMultipartParts`       | Maximum number of multipart fields and files                |
| `maxMultipartHeaderSize`  | Maximum header size of each multipart part                  |
| `maxHeaderSize`           | Maximum size of HTTP headers                                |
| `maxWorkers`              | Maximum number of client connections processed concurrently |
| `readTimeout`             | Maximum time the server waits for additional request data   |
| `writeTimeout`            | Maximum time allowed for writing one complete response      |
| `handlerTimeout`          | Maximum time allowed for application request handling       |
| `handler`                 | Application-specific request handler                        |
| `errorPage`               | Generator used for standard HTML error pages                |
| `sslOptions`              | SSL/TLS configuration; absent for ordinary HTTP             |

For example, limits can be adjusted for an application that accepts larger uploads:

```java
final Options options = new Options.Builder()
    .setMaxRequestSize(256 * 1024 * 1024)
    .setMaxFileSize(128 * 1024 * 1024)
    .setMaxInMemoryBodySize(64 * 1024)
    .setMaxFormSize(1024 * 1024)
    .setMaxMultipartParts(1000)
    .setMaxMultipartHeaderSize(16 * 1024)
    .build();
```

Connection and handler timeouts use `Duration`, making their meaning explicit:

```java
final Options options = new Options.Builder()
    .setReadTimeout(Duration.ofSeconds(20))
    .setWriteTimeout(Duration.ofSeconds(20))
    .setHandlerTimeout(Duration.ofSeconds(5))
    .build();
```

`maxWorkers` limits the number of connections actively processed at the same time. Persistent HTTP connections occupy a worker for their lifetime, so the limit also prevents large numbers of idle or slow clients from consuming server resources without bound.

By default, the server binds to the wildcard address and is therefore reachable
through every local network interface allowed by the host firewall. Applications
that are intended to be reached only through a reverse proxy on the same host
should bind explicitly to a loopback address:

```java
final Options options = new Options.Builder()
    .setBindAddress(InetAddress.getLoopbackAddress())
    .build();
```

`backlog` is a request to the operating system for the maximum number of TCP
connections waiting to be accepted. The operating system may cap or otherwise
adjust it. It is separate from `maxWorkers`: when all workers are occupied, new
connections remain in the operating-system accept queue until worker capacity is
available. Stopping the server remains responsive while the worker limit is
saturated.

Most applications should start with the defaults and change individual settings only when there is a concrete reason to do so. Configuration options are limits and behavior controls, not a checklist that must be ceremonially filled out before the server agrees to function.

## Custom Error Pages

The server provides default HTML pages for errors such as `404 Not Found`, `403 Forbidden`, and `500 Internal Server Error`.

Their appearance can be replaced without changing the HTTP error handling itself. A custom error page is defined through the `ErrorPage` interface.

An error page receives the HTTP status code, its reason, and an explanatory message, and returns HTML:

```java
final ErrorPage errorPage = (code, reason, message) ->
    """
    <!DOCTYPE html>
    <html>
        <head>
            <title>%d %s</title>
        </head>
        <body>
            <h1>%d %s</h1>
            <p>%s</p>
        </body>
    </html>
    """.formatted(
        code,
        reason,
        code,
        reason,
        message
    );
```

It can then be installed through the server options:

```java
final Options options = new Options.Builder()
    .setErrorPage(errorPage)
    .build();
```

The configured page is used by standard server-generated error responses. Application code can continue to return ordinary responses:

```java
return environment
    .getResponseFactory()
    .notFound();
```

The HTTP semantics remain unchanged. A customized `404` page is still returned with the `404 Not Found` status, and an internal server error remains `500 Internal Server Error`. `ErrorPage` controls presentation, not protocol behavior.

This makes it possible to give error pages the same appearance as the rest of an application without requiring every handler to construct its own error responses.

### Running HTTP and HTTPS Together

If an application needs to accept both HTTP and HTTPS connections, simply start two server instances on different ports and give them the same handler:

```java
final Handler handler = (request, environment) ->
    environment
        .getResponseFactory()
        .fromText("Hello, world!")
        .build();

final Server httpServer = Server.start(
    new Options.Builder()
        .setPort(8080)
        .setHandler(handler)
        .build()
);

final Server httpsServer = Server.start(
    new Options.Builder()
        .setPort(8443)
        .setHandler(handler)
        .setSslOptions(sslOptions)
        .build()
);
````

Both servers are completely independent at the transport level while sharing the same application logic. There is no special dual-protocol mode to configure: one server listens for HTTP, another listens for HTTPS, and both use the same `Handler`.

## Connections, Limits, and Timeouts

The server is designed to keep connection management predictable while protecting itself from clients that are slow, idle, malformed, or simply determined to consume resources forever.

### Persistent Connections

HTTP persistent connections are supported automatically.

For HTTP/1.1, connections are persistent by default. A client can explicitly request that the connection be closed:

```http
Connection: close
````

For HTTP/1.0, connections are closed by default and can be kept open explicitly:

```http
Connection: keep-alive
```

The response mirrors the resulting transport decision: a connection that will
close carries `Connection: close`, while an accepted HTTP/1.0 persistent
connection carries `Connection: keep-alive`. HTTP/1.1 persistent responses do
not need a `Connection` header. If a request contains both options, `close`
takes precedence.

A persistent connection can carry multiple requests sequentially. Each request is parsed and processed independently while the underlying TCP connection remains open.

Application handlers do not need to manage any of this. They receive one `Request` at a time and return one `Response`.

### Worker Limit

Each active client connection is processed by a worker. The maximum number of simultaneously active workers can be configured:

```java
final Options options = new Options.Builder()
    .setMaxWorkers(200)
    .build();
```

The limit applies to connections rather than individual HTTP requests. A persistent connection therefore occupies its worker until the connection is closed.

This provides a simple upper bound on the amount of concurrent connection processing performed by the server.

### Read Timeout

A client is not allowed to keep a connection occupied indefinitely while sending no data or only part of a request.

The read timeout controls how long the server waits for additional request data:

```java
final Options options = new Options.Builder()
    .setReadTimeout(Duration.ofSeconds(15))
    .build();
```

If the timeout expires while the server is waiting for request data, the connection is closed.

### Write Timeout

A client that stops reading a response is not allowed to occupy a worker indefinitely either:

```java
final Options options = new Options.Builder()
    .setWriteTimeout(Duration.ofSeconds(15))
    .build();
```

The timeout covers writing and flushing one complete response. If it expires, the server closes the client connection immediately. The worker then returns to the pool and can process another connection; no error response is attempted on the connection that has already stopped consuming output.

### Handler Timeout

Application code is also given a configurable execution limit:

```java
final Options options = new Options.Builder()
    .setHandlerTimeout(Duration.ofSeconds(5))
    .build();
```

If a handler does not complete within that period, the server stops waiting for it and generates an error response.

The timeout prevents a slow or malfunctioning handler from occupying a worker indefinitely. As with normal Java thread interruption, application code should cooperate with interruption when performing long-running operations.

### Size Limits

Several independent limits protect request processing:

```java
final Options options = new Options.Builder()
    .setMaxHeaderSize(64 * 1024)
    .setMaxRequestSize(128 * 1024 * 1024)
    .setMaxFileSize(32 * 1024 * 1024)
    .build();
```

The header limit restricts HTTP header data, the request limit restricts the complete incoming request, and the file limit applies independently to each uploaded file.

These limits are deliberately separate. An application may, for example, accept a large multipart request containing several reasonably sized files without allowing a single uploaded file to consume the entire permitted request size.

## HTTP Support

Foundry19 Web Server implements the parts of HTTP needed for straightforward web applications while deliberately avoiding unnecessary protocol machinery in the public API.

### HTTP Versions

The server supports:

* **HTTP/1.0**
* **HTTP/1.1**

The HTTP version is parsed from each request and used when generating the corresponding response.

Persistent connection behavior follows the version of the incoming request: HTTP/1.1 uses persistent connections by default, while HTTP/1.0 closes them by default unless keep-alive is explicitly requested.

### Request Methods

The server supports ordinary HTTP requests including `GET` and `POST`.

Method names are parsed as case-sensitive HTTP tokens. A valid but unsupported
method receives `501 Not Implemented`; malformed method syntax receives
`400 Bad Request`.

`GET` requests can contain URL query parameters:

```http
GET /search?q=java HTTP/1.1
```

`POST` requests can contain raw request bodies, URL-encoded forms, or multipart form data including uploaded files.

### Request Bodies

Request bodies with a known content length are read according to the `Content-Length` header. The server respects the exact request boundary, which is particularly important for persistent connections where another HTTP request may immediately follow the current request body. Every body is exposed as `UploadedData`; small bodies use memory and larger bodies use temporary-file storage according to `maxInMemoryBodySize`.

`Content-Length` accepts decimal digits only. Requests that combine it with
`Transfer-Encoding` are rejected as ambiguous, and unsupported transfer
codings receive `501 Not Implemented`. HTTP/1.1 clients may use
`Expect: 100-continue`; other expectations receive `417 Expectation Failed`.

URL-encoded form data is parsed automatically:

```http
Content-Type: application/x-www-form-urlencoded
```

Multipart forms and file uploads are also supported:

```http
Content-Type: multipart/form-data; boundary=...
```

Unknown content types can still be handled as binary request data.

### Headers and Cookies

HTTP headers are parsed and made available through the request API. Repeated header and parameter values are preserved where appropriate.

Incoming `Cookie` headers are parsed into structured cookie values, while response cookies can be generated through `ResponseBuilder`.

Response framing, content metadata, and connection headers are generated from
the actual response and transport decision. Application code cannot override
them with conflicting custom values.

### Static Content

Static files can be served directly from the configured web root. Their content type is determined from the file extension, while unknown file types are treated as binary content.

Missing files produce `404 Not Found`, while directories and inaccessible files produce `403 Forbidden`.

### Deliberate Scope

The server is intentionally focused on conventional HTTP/1.0 and HTTP/1.1 request-response processing. It is not intended to implement every protocol and extension that has accumulated around HTTP over the decades.

Features outside the current scope should not be assumed to be supported unless explicitly documented.

This limited scope is intentional: the library aims to provide a small and understandable HTTP server rather than gradually evolving into an application server whose configuration manual weighs more than the application using it.

## Examples

The following examples show several common ways to use the server. They are deliberately small: application code should describe application behavior, not spend half its life negotiating with the web framework.

### A Complete Minimal Application

The following program starts an HTTP server on port `8080` and handles a small API while leaving all other requests to the server's static file handling:

```java
import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.ResponseFactory;
import com.kniazkov.webserver.Server;

public final class Main {

    public static void main(final String[] args) throws Exception {
        final Handler handler = (request, environment) -> {
            final ResponseFactory factory =
                environment.getResponseFactory();

            if (request.getPath().getFullPath().equals("/api/status")) {
                return factory
                    .fromJson("{\"status\":\"ok\"}")
                    .build();
            }

            return factory.noResponse();
        };

        final Options options = new Options.Builder()
            .setPort(8080)
            .setWwwRoot("public")
            .setHandler(handler)
            .build();

        final Server server = Server.start(options);
    }
}
````

A request to:

```text
http://localhost:8080/api/status
```

is handled by application code.

A request such as:

```text
http://localhost:8080/index.html
```

falls through to the default handler and is resolved relative to the configured `public` directory.

### Reading Query Parameters

Query parameters are already decoded and grouped by name when the handler receives the request:

```java
final Handler handler = (request, environment) -> {
    final ResponseFactory factory =
        environment.getResponseFactory();

    if (!request.getPath().getFullPath().equals("/hello")) {
        return factory.noResponse();
    }

    final String name = request
        .getQuery()
        .getOrDefault("name", List.of("World"))
        .getFirst();

    return factory
        .fromText("Hello, " + name + "!")
        .build();
};
```

A request to:

```text
/hello?name=Ivan
```

produces:

```text
Hello, Ivan!
```

while `/hello` produces `Hello, World!`.

### Processing a POST Form

URL-encoded form fields are available through the request without manually reading or decoding the body:

```java
final Handler handler = (request, environment) -> {
    final ResponseFactory factory =
        environment.getResponseFactory();

    if (!request.getPath().getFullPath().equals("/login")) {
        return factory.noResponse();
    }

    final String username = request
        .getForm()
        .getOrDefault("username", List.of(""))
        .getFirst();

    return factory
        .fromJson(
            """
            {"user":"%s"}
            """.formatted(username)
        )
        .build();
};
```

For a request containing:

```text
username=Ivan
```

the form parser has already converted the body into structured values before the handler is invoked.

### Handling an Uploaded File

Multipart uploads follow the same model:

```java
final Handler handler = (request, environment) -> {
    final ResponseFactory factory =
        environment.getResponseFactory();

    final List<UploadedFile> files =
        request.getFiles().get("file");

    if (files == null || files.isEmpty()) {
        return factory
            .fromText("No file uploaded")
            .build();
    }

    final UploadedFile file = files.getFirst();

    return factory
        .fromText(
            "Received: " + file.getName()
        )
        .build();
};
```

Multipart parsing, boundaries, disposition headers, and binary body handling are performed before application code sees the request. Call `openStream()`, `transferTo(...)`, or `readAllBytes()` while the handler is running; temporary upload storage is released when the handler completes.

### Returning Different Response Types

The same handler can return different kinds of content:

```java
final Handler handler = (request, environment) -> {
    final ResponseFactory factory =
        environment.getResponseFactory();

    return switch (request.getPath().getFullPath()) {
        case "/text" ->
            factory
                .fromText("Plain text")
                .build();

        case "/html" ->
            factory
                .fromHtml("<h1>Hello!</h1>")
                .build();

        case "/json" ->
            factory
                .fromJson("{\"message\":\"Hello!\"}")
                .build();

        default ->
            factory.noResponse();
    };
};
```

The appropriate content type is selected by the response factory.

### Combining an API with Static Files

One of the simplest useful configurations is to handle `/api/...` in Java and serve everything else from disk:

```java
final Handler handler = (request, environment) -> {
    final ResponseFactory factory =
        environment.getResponseFactory();

    if (request.getPath().getFullPath().equals("/api/version")) {
        return factory
            .fromJson("{\"version\":\"2.0.0\"}")
            .build();
    }

    if (request.getPath().getFullPath().equals("/api/health")) {
        return factory
            .fromText("OK")
            .build();
    }

    return factory.noResponse();
};

final Server server = Server.start(
    new Options.Builder()
        .setPort(8080)
        .setWwwRoot("public")
        .setHandler(handler)
        .build()
);
```

With a directory such as:

```text
public/
├── index.html
├── application.js
├── styles.css
└── images/
    └── logo.png
```

the same server can provide both application endpoints and ordinary web resources:

```text
/api/version        -> handled by Java code
/api/health         -> handled by Java code
/                   -> default static-file processing
/index.html         -> public/index.html
/styles.css         -> public/styles.css
/images/logo.png    -> public/images/logo.png
```

The handler only handles what belongs to the application. Everything else can remain the server's problem, which is generally a healthier division of labor.

## Building and Testing

The project is built with Maven and requires Java 21.

To compile the project and run the complete test suite:

```bash
mvn test
````

To create the library package:

```bash
mvn package
```

The test suite covers the individual protocol components as well as their interaction at higher levels. This includes request parsing, headers, forms, multipart uploads, files, responses, persistent connections, timeouts, worker limits, and real TCP socket communication.

The project also uses end-to-end tests to verify the server as a complete system from the perspective of an actual client.

Tests are run automatically by the project's continuous integration workflow, so changes are checked against the complete test suite before being accepted.

### End-to-End Tests

End-to-end tests use Playwright and a real Chromium browser. Before running them for the first time, install the Chromium version required by Playwright:

```bash
mvn exec:java -e "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium" "-Dexec.classpathScope=test"
```

The browser installation only needs to be repeated when required by a Playwright upgrade.

To run the complete test suite, including end-to-end tests:

```bash
mvn verify -Pe2e
```

## Project Status and License

Foundry19 Web Server 2.x is the second generation of the library and represents a substantial redesign of its API and internal architecture.

The project targets Java 21 and focuses on providing a compact HTTP/HTTPS server for applications that need direct and understandable control over request handling without introducing a full web framework.

The public API is intentionally kept small. New functionality should extend that model rather than turn the library into an application framework with its own opinions about how the rest of the program must be structured.

As the project evolves, compatibility and behavior changes will be documented with each release.

### License

This project is licensed under the MIT License.

See the `LICENSE` file in the project repository for the complete license terms.
