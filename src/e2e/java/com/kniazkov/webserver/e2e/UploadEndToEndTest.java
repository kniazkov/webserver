/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.UploadedFile;

import com.microsoft.playwright.Page;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end tests for multipart file uploads.
 */
final class UploadEndToEndTest extends EndToEndBaseTest {

    /**
     * Forces multipart request bodies through temporary-file storage.
     *
     * @param builder
     *     the server options builder.
     */
    @Override
    protected void configure(final Options.Builder builder) {
        builder.setMaxInMemoryBodySize(0);
        super.configure(builder);
    }

    /**
     * Tests uploading one text file through an HTML form.
     */
    @Test
    void uploadFile() throws Exception {
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

        final byte[] data = "Hello from uploaded file".getBytes();

        final Path file = Files.createTempFile(
            "webserver-upload-",
            ".txt"
        );

        try {
            Files.write(file, data);

            final Handler handler = (request, environment) -> {
                if (!request.getPath().getPath().equals("/upload")) {
                    return environment
                        .getResponseFactory()
                        .noResponse();
                }

                final List<UploadedFile> files =
                    request.getFiles().get("file");

                assertNotNull(files);
                assertEquals(1, files.size());

                final UploadedFile uploaded = files.getFirst();

                assertEquals(
                    file.getFileName().toString(),
                    uploaded.getName()
                );
                assertArrayEquals(
                    data,
                    uploaded.readAllBytes()
                );

                return environment
                    .getResponseFactory()
                    .fromHtml("<h1>Uploaded</h1>")
                    .build();
            };

            startServer(handler);

            page.navigate(url("/upload.html"));

            page.locator("input[type=file]")
                .setInputFiles(file);

            page.getByRole(
                com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Upload")
            ).click();

            assertThat(page.locator("h1"))
                .hasText("Uploaded");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * Tests uploading several files through the same form field.
     */
    @Test
    void uploadSeveralFiles() throws Exception {
        writeFile(
            "upload.html",
            """
            <!DOCTYPE html>
            <html>
            <body>
                <form action="/upload"
                      method="post"
                      enctype="multipart/form-data">
                    <input type="file" name="files" multiple>
                    <button type="submit">Upload</button>
                </form>
            </body>
            </html>
            """
        );

        final Path first = Files.createTempFile(
            "webserver-first-",
            ".txt"
        );

        final Path second = Files.createTempFile(
            "webserver-second-",
            ".txt"
        );

        try {
            Files.writeString(first, "first");
            Files.writeString(second, "second");

            final Handler handler = (request, environment) -> {
                if (!request.getPath().getPath().equals("/upload")) {
                    return environment
                        .getResponseFactory()
                        .noResponse();
                }

                final List<UploadedFile> files =
                    request.getFiles().get("files");

                assertNotNull(files);
                assertEquals(2, files.size());

                assertEquals(
                    first.getFileName().toString(),
                    files.get(0).getName()
                );

                assertEquals(
                    second.getFileName().toString(),
                    files.get(1).getName()
                );

                return environment
                    .getResponseFactory()
                    .fromText("OK")
                    .build();
            };

            startServer(handler);

            page.navigate(url("/upload.html"));

            page.locator("input[type=file]")
                .setInputFiles(new Path[] {
                    first,
                    second
                });

            page.getByRole(
                com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Upload")
            ).click();

            assertEquals("OK", page.locator("body").innerText());
        } finally {
            Files.deleteIfExists(first);
            Files.deleteIfExists(second);
        }
    }

    /**
     * Tests a form containing both ordinary fields and an uploaded file.
     */
    @Test
    void uploadFileWithFormData() throws Exception {
        writeFile(
            "upload.html",
            """
            <!DOCTYPE html>
            <html>
            <body>
                <form action="/upload"
                      method="post"
                      enctype="multipart/form-data">
                    <input name="title" value="Example">
                    <input type="file" name="file">
                    <button type="submit">Upload</button>
                </form>
            </body>
            </html>
            """
        );

        final Path file = Files.createTempFile(
            "webserver-upload-",
            ".bin"
        );

        try {
            Files.write(
                file,
                new byte[] {
                    0,
                    1,
                    2,
                    (byte) 0xff
                }
            );

            final Handler handler = (request, environment) -> {
                if (!request.getPath().getPath().equals("/upload")) {
                    return environment
                        .getResponseFactory()
                        .noResponse();
                }

                assertEquals(
                    "Example",
                    request.getForm()
                        .get("title")
                        .getFirst()
                );

                assertEquals(
                    1,
                    request.getFiles()
                        .get("file")
                        .size()
                );

                return environment
                    .getResponseFactory()
                    .fromText("OK")
                    .build();
            };

            startServer(handler);

            page.navigate(url("/upload.html"));

            page.locator("input[type=file]")
                .setInputFiles(file);

            page.getByRole(
                com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Upload")
            ).click();

            assertEquals("OK", page.locator("body").innerText());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
