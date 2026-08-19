/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Handler;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for query strings and URL-encoded forms.
 */
final class FormsEndToEndTest extends EndToEndBaseTest {

    /**
     * Tests reading query parameters from a GET request.
     */
    @Test
    void queryString() throws Exception {
        final Handler handler = (request, environment) -> {
            assertEquals(
                Map.of(
                    "name", List.of("Ivan"),
                    "language", List.of("Java")
                ),
                request.getQuery()
            );

            return environment
                .getResponseFactory()
                .fromText("OK")
                .build();
        };

        startServer(handler);

        final var response = page.request().get(
            url("/search?name=Ivan&language=Java")
        );

        assertEquals(200, response.status());
        assertEquals("OK", response.text());
    }

    /**
     * Tests repeated query parameters.
     */
    @Test
    void repeatedQueryParameters() throws Exception {
        final Handler handler = (request, environment) -> {
            assertEquals(
                List.of("java", "http", "server"),
                request.getQuery().get("tag")
            );

            return environment
                .getResponseFactory()
                .fromText("OK")
                .build();
        };

        startServer(handler);

        final var response = page.request().get(
            url("/search?tag=java&tag=http&tag=server")
        );

        assertEquals(200, response.status());
    }

    /**
     * Tests URL decoding in a query string.
     */
    @Test
    void encodedQueryString() throws Exception {
        final Handler handler = (request, environment) -> {
            assertEquals(
                "hello world",
                request.getQuery().get("q").getFirst()
            );

            assertEquals(
                "Иван",
                request.getQuery().get("name").getFirst()
            );

            return environment
                .getResponseFactory()
                .fromText("OK")
                .build();
        };

        startServer(handler);

        final var response = page.request().get(
            url(
                "/search?q=hello+world"
                    + "&name=%D0%98%D0%B2%D0%B0%D0%BD"
            )
        );

        assertEquals(200, response.status());
    }

    /**
     * Tests submitting a URL-encoded HTML form through a real browser.
     */
    @Test
    void submitForm() throws Exception {
        writeFile(
            "form.html",
            """
            <!DOCTYPE html>
            <html>
            <body>
                <form action="/submit" method="post">
                    <input name="name" value="Ivan">
                    <input name="language" value="Java">
                    <button type="submit">Submit</button>
                </form>
            </body>
            </html>
            """
        );

        final Handler handler = (request, environment) -> {
            if (!request.getPath().getPath().equals("/submit")) {
                return environment
                    .getResponseFactory()
                    .noResponse();
            }

            assertEquals(
                Map.of(
                    "name", List.of("Ivan"),
                    "language", List.of("Java")
                ),
                request.getForm()
            );

            return environment
                .getResponseFactory()
                .fromHtml(
                    "<html><body><h1>Saved</h1></body></html>"
                )
                .build();
        };

        startServer(handler);

        page.navigate(url("/form.html"));
        page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Submit")
        ).click();

        assertEquals(
            "Saved",
            page.locator("h1").textContent()
        );
    }

    /**
     * Tests repeated fields in a URL-encoded form.
     */
    @Test
    void repeatedFormFields() throws Exception {
        final Handler handler = (request, environment) -> {
            assertEquals(
                List.of("java", "http"),
                request.getForm().get("tag")
            );

            return environment
                .getResponseFactory()
                .fromText("OK")
                .build();
        };

        startServer(handler);

        final var response = page.request().post(
            url("/submit"),
            RequestOptions.create()
                .setHeader(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                .setData("tag=java&tag=http")
        );

        assertEquals(200, response.status());
    }

    /**
     * Tests query parameters and form fields in the same POST request.
     */
    @Test
    void queryAndFormTogether() throws Exception {
        final Handler handler = (request, environment) -> {
            assertEquals(
                "test",
                request.getQuery().get("source").getFirst()
            );

            assertEquals(
                "Ivan",
                request.getForm().get("name").getFirst()
            );

            return environment
                .getResponseFactory()
                .fromText("OK")
                .build();
        };

        startServer(handler);

        final var response = page.request().post(
            url("/submit?source=test"),
            RequestOptions.create()
                .setHeader(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                .setData("name=Ivan")
        );

        assertEquals(200, response.status());
    }

    /**
     * Tests a form containing an empty field.
     */
    @Test
    void emptyFormValue() throws Exception {
        final Handler handler = (request, environment) -> {
            assertEquals(
                "",
                request.getForm().get("comment").getFirst()
            );

            return environment
                .getResponseFactory()
                .fromText("OK")
                .build();
        };

        startServer(handler);

        final var response = page.request().post(
            url("/submit"),
            RequestOptions.create()
                .setHeader(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                )
                .setData("comment=")
        );

        assertEquals(200, response.status());
    }
}
