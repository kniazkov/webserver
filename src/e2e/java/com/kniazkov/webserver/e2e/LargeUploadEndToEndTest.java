/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Options;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for file upload size limits.
 */
final class LargeUploadEndToEndTest extends EndToEndBaseTest {

    /**
     * Configures restrictive upload limits for this test class.
     *
     * @param builder
     *     the server options builder.
     */
    @Override
    protected void configure(final Options.Builder builder) {
        builder
            .setMaxFileSize(1024)
            .setMaxRequestSize(16 * 1024);
        super.configure(builder);
    }

    /**
     * Tests rejection of a file exceeding the configured maximum size.
     */
    @Test
    void fileTooLarge() throws Exception {
        writeFile(
            "upload.html",
            """
            <!DOCTYPE html>
            <html>
            <body>
                <form action="/upload"
                      method="post"
                      enctype="multipart/form-data">
                    <input type="file" name="file">
                    <button type="submit">Upload</button>
                </form>
            </body>
            </html>
            """
        );

        final Path file = Files.createTempFile(
            "webserver-large-upload-",
            ".bin"
        );

        try {
            /*
             * The file is deliberately larger than the configured
             * one-kilobyte limit.
             */
            Files.write(
                file,
                new byte[4 * 1024]
            );

            startServer(
                (request, environment) -> {
                    if (!request.getPath().getPath().equals("/upload")) {
                        return environment
                            .getResponseFactory()
                            .noResponse();
                    }

                    return environment
                        .getResponseFactory()
                        .fromText("This must not be reached")
                        .build();
                }
            );

            page.navigate(url("/upload.html"));

            page.locator("input[type=file]")
                .setInputFiles(file);

            final var response = page.waitForResponse(
                value -> value.url().endsWith("/upload"),
                () -> page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Upload")
                ).click()
            );

            assertEquals(413, response.status());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
