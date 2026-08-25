# Code style

This document describes the conventions used by the current implementation.
New code must pass `mvn verify`; the rules that can be checked mechanically are
also enforced by Checkstyle and the Java compiler in that build.

## General formatting

- Use UTF-8, Unix line endings, four spaces for indentation, and no tabs.
- Keep lines at or below 100 characters.
- Do not put more than one statement on a line.
- Use braces for every control-flow body, including single-statement bodies.
- Do not use wildcard imports or trailing comments.
- Declare parameters and local variables `final` unless reassignment is part of
  the algorithm.

The repository contains an `.editorconfig` file with the editor-level rules.

## Naming and structure

- Use standard Java naming: `UpperCamelCase` for types, `lowerCamelCase` for
  methods and variables, and `UPPER_SNAKE_CASE` for constants.
- Prefer one top-level type per source file and give the file the same name as
  that type.
- Keep implementation details package-private or private. Expose only behavior
  that belongs to the public API.
- Prefer immutable values. Copy mutable input and do not expose internal mutable
  collections or arrays.
- Use `Optional` only as a return type. Do not use it for fields or parameters.
- Avoid `null` in the public API. Reject invalid arguments at the boundary.

## Documentation and comments

- Add Javadocs to types, fields, constructors, and methods, including tests.
- Document API contracts, edge cases, ownership, and thrown exceptions rather
  than restating the implementation.
- Every Javadoc or explanatory comment occupies at least three lines. Do not use
  `//` comments or one-line block comments.
- Keep test Javadocs short and describe the behavior being verified.

For example:

```java
/**
 * The default maximum number of multipart parts.
 */
private static final int DEFAULT_MAX_MULTIPART_PARTS = 1000;
```

## Public API

- Use interfaces for public abstractions and keep concrete implementations out
  of the exported API unless callers need to construct them directly.
- Preserve source and behavioral compatibility unless a breaking change is
  deliberate and documented.
- Prefer domain types such as `HttpStatus` and `ContentType` to unvalidated
  strings and integers.
- Report recoverable server failures with `ServerException` and preserve their
  causes. Attach an HTTP status when the failure has a meaningful response.

## Builders

- A builder validates its complete state in `build()` and either returns a
  usable immutable object or throws the documented exception.
- Fluent setters return the same builder instance.
- Preset factory methods must lock the invariants they establish. If a factory
  creates a text response, callers may add headers but may not replace its body,
  content type, or status.
- Keep explicitly unsafe or fully custom construction behind a single clearly
  named entry point.

The project intentionally uses nested builders with `build()`. The generic
`Builder<T>`/`create()` convention from the previous implementation is not part
of the current API.

## Errors and resources

- Never swallow an exception unless the operation is explicitly best-effort and
  a nearby block comment explains why no recovery is possible.
- Use try-with-resources for owned resources. Define and document ownership at
  API boundaries.
- Do not expose request data or parser diagnostics in client-facing errors.

## Tests

- Add unit tests for normal, boundary, and failure behavior.
- Add end-to-end tests when behavior depends on sockets, TLS, HTTP framing, or
  browser-visible semantics.
- Tests must be deterministic and must release sockets, executors, temporary
  files, and other resources.
- Run the complete local gate with `mvn verify -Pe2e`.

## Suppressions

Do not weaken a repository-wide rule to accommodate one exceptional construct.
Use the narrowest possible suppression, explain it with a multi-line comment,
and keep the suppressed region small.
