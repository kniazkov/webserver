/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RequestHeaderTest {
    @Test
    void buildsACompleteRequestHeader() {
        final RequestHeader header = RequestHeader.builder()
                .method("GET")
                .requestTarget("/search?q=java")
                .httpVersion(1, 1)
                .addHeader("Host", "example.test")
                .addHeader("Accept", "text/html")
                .addHeader("accept", "application/json")
                .build();

        assertAll(
                () -> assertEquals("GET", header.method()),
                () -> assertEquals("/search?q=java", header.requestTarget()),
                () -> assertEquals(1, header.httpMajorVersion()),
                () -> assertEquals(1, header.httpMinorVersion()),
                () -> assertEquals(List.of("text/html", "application/json"),
                        header.headerValues("ACCEPT")),
                () -> assertEquals("example.test", header.firstHeaderValue("host").orElseThrow()),
                () -> assertEquals(List.of("example.test"), header.headers().get("HOST"))
        );
    }

    @Test
    void createsADeeplyImmutableSnapshot() {
        final RequestHeader.Builder builder = RequestHeader.builder()
                .method("GET")
                .requestTarget("/")
                .httpVersion(1, 1)
                .addHeader("X-Test", "one");
        final RequestHeader header = builder.build();

        builder.addHeader("X-Test", "two");

        assertAll(
                () -> assertEquals(List.of("one"), header.headerValues("x-test")),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> header.headers().put("other", List.of("value"))),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> header.headerValues("x-test").add("value"))
        );
    }

    @Test
    void rejectsAnIncompleteRequestLine() {
        assertAll(
                () -> assertThrows(IllegalStateException.class,
                        () -> RequestHeader.builder()
                                .requestTarget("/")
                                .httpVersion(1, 1)
                                .build()),
                () -> assertThrows(IllegalStateException.class,
                        () -> RequestHeader.builder()
                                .method("GET")
                                .httpVersion(1, 1)
                                .build()),
                () -> assertThrows(IllegalStateException.class,
                        () -> RequestHeader.builder()
                                .method("GET")
                                .requestTarget("/")
                                .build())
        );
    }

    @Test
    void rejectsInvalidTokensAndControlCharacters() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.builder().method("NOT VALID")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.builder().requestTarget("/not valid")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.builder().addHeader("Bad Header", "value")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.builder().addHeader("X-Test", "value\r\ninjected")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RequestHeader.builder().httpVersion(-1, 1))
        );
    }
}
