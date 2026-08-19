/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Options;

import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for custom error pages.
 */
final class CustomErrorPageEndToEndTest
    extends EndToEndBaseTest {

    /**
     * Configures a custom error page.
     *
     * @param builder
     *     the server options builder.
     */
    @Override
    protected void configure(final Options.Builder builder) {
        builder.setErrorPage(
            (code, reason, message) ->
                """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1 id="code">%d</h1>
                    <div id="reason">%s</div>
                    <div id="message">%s</div>
                </body>
                </html>
                """.formatted(
                    code,
                    reason,
                    message
                )
        );
        super.configure(builder);
    }

    /**
     * Tests rendering a custom 404 page.
     */
    @Test
    void customNotFoundPage() throws Exception {
        startServer();

        final var response =
            page.navigate(url("/does-not-exist.html"));

        assertEquals(404, response.status());

        assertThat(page.locator("#code"))
            .hasText("404");

        assertThat(page.locator("#reason"))
            .hasText("Not Found");
    }
}
