# Webserver

Version 2 is a from-scratch rewrite of Webserver: a small, embeddable HTTP server for Java applications.

> **Status:** early development. This branch currently contains only the project skeleton; there is no public API or usable server implementation yet.

## Goals

- A small and straightforward embedded API.
- Correct HTTP behavior.
- Secure defaults and explicit resource limits.
- Predictable lifecycle and concurrency.
- Comprehensive unit, integration, and raw-socket tests.

## Requirements

- JDK 17 or newer.
- Apache Maven 3.9 or newer.

## Build

```shell
mvn verify
```

Production code belongs in `src/main/java`; tests belong in `src/test/java`.

The previous implementation remains available on the `master` branch.
