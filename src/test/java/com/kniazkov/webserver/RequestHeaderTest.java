/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the observable contract of {@link RequestHeader}.
 */
final class RequestHeaderTest {
    @Test
    void createReturnsCompleteHeaderAndPreservesCaseInsensitiveRepeatedValues() {
        final RequestHeader header = RequestHeader.createBuilder()
                .setMethod("GET")
                .setRequestTarget("/search?q=java")
                .setHttpVersion(1, 1)
                .addHeader("Host", "example.test")
                .addHeader("Accept", "text/html")
                .addHeader("accept", "application/json")
                .create();

        assertAll(
                () -> assertEquals("GET", header.getMethod()),
                () -> assertEquals("/search?q=java", header.getRequestTarget()),
                () -> assertEquals(1, header.getHttpMajorVersion()),
                () -> assertEquals(1, header.getHttpMinorVersion()),
                () -> assertEquals(List.of("text/html", "application/json"),
                        header.getHeaderValues("ACCEPT")),
                () -> assertEquals("example.test",
                        header.getFirstHeaderValue("host").orElseThrow()),
                () -> assertEquals(List.of("example.test"), header.getHeaders().get("HOST")),
                () -> assertEquals(List.of(), header.getHeaderValues("missing")),
                () -> assertFalse(header.getFirstHeaderValue("missing").isPresent())
        );
    }

    @Test
    void createReturnsDeeplyImmutableSnapshotIndependentFromFurtherBuilderChanges() {
        final RequestHeader.Builder builder = RequestHeader.createBuilder()
                .setMethod("GET")
                .setRequestTarget("/")
                .setHttpVersion(1, 1)
                .addHeader("X-Test", "one");
        final RequestHeader header = builder.create();

        builder.addHeader("X-Test", "two");

        assertAll(
                () -> assertEquals(List.of("one"), header.getHeaderValues("x-test")),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> header.getHeaders().put("other", List.of("value"))),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> header.getHeaderValues("x-test").add("value"))
        );
    }

    @Test
    void builderImplementsGlobalContractAndReportsValidityWithoutCreatingObjects() {
        final RequestHeader.Builder concreteBuilder = RequestHeader.createBuilder();
        final Builder<RequestHeader> builder = concreteBuilder;

        assertFalse(builder.isValid());
        final IllegalStateException exception =
                assertThrows(IllegalStateException.class, builder::create);
        assertEquals("Cannot create RequestHeader: builder is invalid", exception.getMessage());

        concreteBuilder
                .setMethod("GET")
                .setRequestTarget("/")
                .setHttpVersion(1, 1);

        assertTrue(builder.isValid());
        assertEquals("GET", builder.create().getMethod());
    }

    @Test
    void createRejectsEveryMissingRequiredRequestLineComponent() {
        assertAll(
                () -> assertThrows(IllegalStateException.class,
                        () -> RequestHeader.createBuilder()
                                .setRequestTarget("/")
                                .setHttpVersion(1, 1)
                                .create()),
                () -> assertThrows(IllegalStateException.class,
                        () -> RequestHeader.createBuilder()
                                .setMethod("GET")
                                .setHttpVersion(1, 1)
                                .create()),
                () -> assertThrows(IllegalStateException.class,
                        () -> RequestHeader.createBuilder()
                                .setMethod("GET")
                                .setRequestTarget("/")
                                .create())
        );
    }

    @Test
    void builderRejectsInvalidTokensControlCharactersAndVersionComponents() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.createBuilder().setMethod("NOT VALID")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.createBuilder().setRequestTarget("/not valid")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.createBuilder().addHeader("Bad Header", "value")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.createBuilder()
                                .addHeader("X-Test", "value\r\ninjected")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.createBuilder().setHttpVersion(-1, 1))
        );
    }

    @Test
    void publicMethodsRejectNullAtEveryNonNullableBoundary() {
        final RequestHeader header = RequestHeader.createBuilder()
                .setMethod("GET")
                .setRequestTarget("/")
                .setHttpVersion(1, 1)
                .create();

        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> RequestHeader.createBuilder().setMethod(null)),
                () -> assertThrows(NullPointerException.class,
                        () -> RequestHeader.createBuilder().setRequestTarget(null)),
                () -> assertThrows(NullPointerException.class,
                        () -> RequestHeader.createBuilder().addHeader(null, "value")),
                () -> assertThrows(NullPointerException.class,
                        () -> RequestHeader.createBuilder().addHeader("X-Test", null)),
                () -> assertThrows(NullPointerException.class,
                        () -> header.getHeaderValues(null)),
                () -> assertThrows(NullPointerException.class,
                        () -> header.getFirstHeaderValue(null))
        );
    }
}
