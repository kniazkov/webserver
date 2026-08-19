/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Handler;

import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * End-to-end tests for browser cookie handling.
 */
final class CookieRoundTripEndToEndTest
    extends EndToEndBaseTest {

    /**
     * Tests sending a response cookie and receiving it in the next request.
     */
    @Test
    void cookieRoundTrip() throws Exception {
        final Handler handler = (request, environment) -> {
            final var factory =
                environment.getResponseFactory();

            if (
                request.getPath()
                    .getPath()
                    .equals("/set-cookie")
            ) {
                return factory
                    .fromHtml("<h1>Cookie set</h1>")
                    .setCookie("session", "abc123")
                    .build();
            }

            if (
                request.getPath()
                    .getPath()
                    .equals("/read-cookie")
            ) {
                final String value =
                    request.getCookies().get("session");

                return factory
                    .fromHtml(
                        "<h1>" + value + "</h1>"
                    )
                    .build();
            }

            return factory.notFound();
        };

        startServer(handler);

        page.navigate(url("/set-cookie"));

        assertThat(page.locator("h1"))
            .hasText("Cookie set");

        page.navigate(url("/read-cookie"));

        assertThat(page.locator("h1"))
            .hasText("abc123");
    }
}
