# Code Style and Engineering Rules

This is a living document. The rules below apply to all new and modified code. Existing code
should be brought into compliance when it is touched.

## Method names

A method name normally starts with a verb and describes what the method does.

Examples include:

- `getHeader` for retrieving a value;
- `isOpen`, `hasBody`, or `canReuse` for a boolean query;
- `addHeader` for adding a value;
- `setTimeout` for changing a value;
- `parseRequestLine`, `validateHeader`, and `build` for behavior specific to the domain.

Do not add a meaningless verb merely to satisfy this rule. Fluent builders and deliberately
record-like accessors may omit a verb when that makes the call site clearer. Prefer a precise
domain verb over vague names such as `process`, `handle`, or `doWork`.

Use `camelCase` for methods, parameters, and local variables, `PascalCase` for types, and
`UPPER_SNAKE_CASE` for constants.

## Null

A public method never returns `null`.

- Return `Optional.empty()` when the absence of one value is a normal result.
- Return an empty collection, map, stream, or array when there are no elements.
- The `Optional` or collection object itself must never be `null`.
- Throw an exception when absence means that an invariant or precondition was violated.

Public methods may accept `null` only when it has a clear, necessary domain meaning. Such
cases must be rare and explicitly documented. Otherwise, reject `null` immediately at the API
boundary.

A private method may return `null` when it materially simplifies complex internal logic and
the value cannot escape through the public API. Do not allocate `Optional` objects throughout
private implementation code solely for stylistic purity.

## Immutability

Prefer immutable objects whenever practical.

- Make fields `final` unless mutation is required.
- Make classes `final`, or otherwise prevent uncontrolled extension, when inheritance is not
  part of the design.
- Validate all required state during construction.
- Defensively copy mutable input.
- Never expose mutable internal collections, arrays, or other state.
- Return unmodifiable views only when later internal mutation is intentionally observable;
  otherwise return an immutable snapshot.
- A builder may be mutable, but each built object must be an independent immutable snapshot.

Mutation is acceptable for objects whose purpose is inherently stateful, such as builders,
buffers, lifecycle controllers, and connection state. Keep that mutation local and make its
ownership clear. Do not distort a simple design merely to claim that every object is immutable.

## JavaDoc and comments

Write JavaDoc for:

- every class, interface, enum, record, annotation, and nested type;
- every field, including private and package-private fields;
- every constructor;
- every method, regardless of visibility, except a method annotated with `@Test` or an overridden
  method whose inherited contract remains fully applicable.

A method annotated with `@Test` never has JavaDoc. Its name must instead be long, specific, and
behavior-oriented so that a failed test explains the broken contract without a separate comment.
This exception does not apply to test classes, fields, constructors, or helper methods.

Document an overridden method when it strengthens or changes the inherited contract, including
nullability, exceptions, side effects, or thread-safety.

Every comment, including a comment containing only one sentence, occupies at least three lines
and uses this form:

```java
/**
 * Description.
 */
```

Do not use end-of-line comments or single-line `//` comments. Documentation uses complete
sentences and explains the contract, intent, non-obvious constraint, or reason behind the code
instead of merely restating the implementation.

Public JavaDoc must describe relevant parameters, return values, exceptions, nullability, side
effects, ownership of returned data, and thread-safety expectations.

## API design and validation

Reject invalid input at the public boundary so that objects cannot enter an invalid state.

Use exception types consistently:

- `NullPointerException` for a required argument that is `null`;
- `IllegalArgumentException` for a non-null argument with an invalid value;
- `IllegalStateException` when an operation cannot run because required state is missing or
  the object is in the wrong lifecycle state.

Do not silently discard, merge, normalize, or reinterpret protocol data unless the protocol
requires it. Preserve unknown extension tokens and repeated values when the protocol permits
them. Model case sensitivity, ordering, and multiplicity explicitly.

Prefer narrow types and small public APIs. Do not expose an implementation detail merely because
it is convenient internally.

## Collections

Use the most specific collection contract that matches the data.

- Preserve order when order has meaning.
- Preserve duplicates when duplicates are valid.
- Do not collapse a multimap into a single-value map.
- Return immutable collections from immutable objects.
- Use plural names for methods and fields that contain multiple values.

## Tests

Tests are required for observable behavior, invariants, validation, conversions, protocol edge
cases, concurrency, lifecycle behavior, and immutability guarantees.

A class that is only a trivial data carrier does not need tests for generated or direct accessors.
Once it contains custom validation, normalization, defensive copying, ordering, case-insensitive
lookup, or another contract, test that contract.

Tests must:

- exercise the public contract rather than private implementation details;
- include failure cases and boundary values;
- prove that immutable results cannot be modified through returned data;
- use long, descriptive, behavior-oriented names instead of JavaDoc on `@Test` methods;
- remain deterministic and independent of execution order.

## General Java style

- Use English for identifiers, JavaDoc, comments, and diagnostic messages.
- Use UTF-8, four spaces for indentation, and no tab characters.
- Do not use wildcard imports.
- Declare parameters and local variables `final` when they are not reassigned, unless doing so
  makes the code materially harder to read.
- Keep methods focused. Extract a helper when it gives a distinct operation a precise name, not
  merely to reduce line count.
- Do not swallow exceptions. Either handle them with a defined recovery policy or propagate an
  appropriate exception with useful context.
- Keep compiler warnings at zero. A warning suppression must be narrow and documented.

## Verification

Run the full verification suite before merging:

```shell
mvn verify
```

A pull request is not ready to merge while required GitHub Actions checks are failing. A
documentation-only change does not require artificial tests, but it must still pass the existing
verification workflow.

This list will grow as the project encounters new design and maintenance decisions.
