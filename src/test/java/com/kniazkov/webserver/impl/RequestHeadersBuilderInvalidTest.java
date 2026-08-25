/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.HttpMethod;
import com.kniazkov.webserver.HttpVersion;
import com.kniazkov.webserver.ServerException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests invalid states and values accepted by {@link RequestHeadersBuilder}.
 */
final class RequestHeadersBuilderInvalidTest {

    /**
     * Tests building headers without an HTTP method.
     */
    @Test
    void missingMethod() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder()
            .setTarget("/")
            .setVersion(HttpVersion.HTTP_1_1);

        assertThrows(ServerException.class, builder::build);
    }

    /**
     * Tests building headers without a request target.
     */
    @Test
    void missingTarget() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setVersion(HttpVersion.HTTP_1_1);

        assertThrows(ServerException.class, builder::build);
    }

    /**
     * Tests building headers with an empty request target.
     */
    @Test
    void emptyTarget() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("")
            .setVersion(HttpVersion.HTTP_1_1);

        assertThrows(ServerException.class, builder::build);
    }

    /**
     * Tests building headers without an HTTP version.
     */
    @Test
    void missingVersion() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("/");

        assertThrows(ServerException.class, builder::build);
    }

    /**
     * Tests adding a header with a null name.
     */
    @Test
    void nullHeaderNameOnAdd() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder();

        assertThrows(
            ServerException.class,
            () -> builder.addValue(null, "value")
        );
    }

    /**
     * Tests setting a header with a null name.
     */
    @Test
    void nullHeaderNameOnSet() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder();

        assertThrows(
            ServerException.class,
            () -> builder.setValue(null, "value")
        );
    }

    /**
     * Tests adding a header with an empty name.
     */
    @Test
    void emptyHeaderNameOnAdd() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder();

        assertThrows(
            ServerException.class,
            () -> builder.addValue("", "value")
        );
    }

    /**
     * Tests setting a header with an empty name.
     */
    @Test
    void emptyHeaderNameOnSet() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder();

        assertThrows(
            ServerException.class,
            () -> builder.setValue("", "value")
        );
    }

    /**
     * Tests invalid characters in header names.
     */
    @Test
    void invalidHeaderNames() {
        final String[] names = {
            "Content Type",
            "Content:Type",
            "Content/Type",
            "Content(Type)",
            "Content@Type",
            "Content,Type",
            "Content;Type",
            "Content=Type",
            "Content?Type",
            "Content\tType",
            "Content\rType",
            "Content\nType"
        };

        for (String name : names) {
            final RequestHeadersBuilder builder = new RequestHeadersBuilder();

            assertThrows(
                ServerException.class,
                () -> builder.addValue(name, "value"),
                name
            );
        }
    }

    /**
     * Tests adding a null header value.
     */
    @Test
    void nullHeaderValueOnAdd() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder();

        assertThrows(
            ServerException.class,
            () -> builder.addValue("Test", null)
        );
    }

    /**
     * Tests setting a null header value.
     */
    @Test
    void nullHeaderValueOnSet() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder();

        assertThrows(
            ServerException.class,
            () -> builder.setValue("Test", null)
        );
    }

    /**
     * Tests forbidden characters in header values.
     */
    @Test
    void invalidHeaderValues() {
        final String[] values = {
            "first\rsecond",
            "first\nsecond",
            "first\r\nsecond",
            "first\0second"
        };

        for (String value : values) {
            final RequestHeadersBuilder builder = new RequestHeadersBuilder();

            assertThrows(
                ServerException.class,
                () -> builder.addValue("Test", value),
                value
            );
        }
    }

    /**
     * Tests a request target that contains only whitespace.
     */
    @Test
    void blankTarget() {
        final RequestHeadersBuilder builder = new RequestHeadersBuilder()
            .setMethod(HttpMethod.GET)
            .setTarget("   ")
            .setVersion(HttpVersion.HTTP_1_1);

        assertThrows(ServerException.class, builder::build);
    }
}
