# Changelog

All notable changes to this project are documented in this file. The project
uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

No changes yet.

## [2.0.0] - 2026-08-25

Version 2.0.0 is a complete redesign and is not source-compatible with 1.x.

### Added

- Embedded HTTP/1.0 and HTTP/1.1 server for Java 21.
- Persistent connections with bounded virtual-thread workers.
- Configurable read, write, and handler timeouts.
- Strict parsing for request lines, headers, paths, cookies, content length,
  URL-encoded forms, and multipart forms.
- Streaming `UploadedData` and `UploadedFile` APIs with memory and temporary-file
  storage implementations.
- Independent limits for requests, headers, forms, multipart metadata, part
  counts, files, and in-memory bodies.
- Typed `HttpStatus` and `ContentType` enums.
- Constrained `ResponseFactory` and `ResponseBuilder` APIs with raw-byte and
  fully custom response escape hatches.
- Validated response headers and response cookies.
- Static-file serving with real-path and symbolic-link containment.
- HTTPS using PKCS #12, JKS, or PEM server identities.
- Configurable TLS protocols, cipher suites, and mutual TLS.
- Unit, socket-level, deterministic fuzz, and Playwright end-to-end tests.
- Checkstyle and compiler-warning gates in the Maven build.

### Changed

- Replaced the 1.x API and implementation with an immutable, builder-based API.
- Removed external runtime dependencies.
- The running server now keeps the JVM alive until `Server.stop()` is called.

[Unreleased]: https://github.com/kniazkov/webserver/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/kniazkov/webserver/releases/tag/v2.0.0
