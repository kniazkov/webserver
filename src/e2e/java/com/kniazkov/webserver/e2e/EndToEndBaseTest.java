/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Server;
import com.kniazkov.webserver.ServerException;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for end-to-end tests.
 * <p>
 * The class manages a real Playwright browser together with an isolated browser
 * context and page for every test. It also provides helpers for starting a real
 * web server on an automatically selected local port.
 * <p>
 * The browser process is shared by all tests extending this class, while each
 * individual test receives its own browser context. Server instances are not
 * shared and are stopped automatically after each test.
 */
abstract class EndToEndBaseTest {

    /**
     * The Playwright instance shared by the test class hierarchy.
     */
    private static Playwright playwright;

    /**
     * The browser shared by the test class hierarchy.
     */
    private static Browser browser;

    /**
     * The browser context used by the current test.
     */
    private BrowserContext context;

    /**
     * The page used by the current test.
     */
    protected Page page;

    /**
     * The server used by the current test.
     */
    private Server server;

    /**
     * The port used by the current test server.
     */
    private int port;

    /**
     * The temporary WWW root used by the current test.
     */
    protected Path wwwRoot;

    /**
     * Creates Playwright and starts the browser.
     */
    @BeforeAll
    static void startBrowser() {
        playwright = Playwright.create();
        browser = playwright
            .chromium()
            .launch();
    }

    /**
     * Creates resources that must be isolated between individual tests.
     *
     * @throws IOException
     *     if the temporary WWW root cannot be created.
     */
    @BeforeEach
    void createTestEnvironment() throws IOException {
        context = browser.newContext(
            new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(true)
        );
        page = context.newPage();
        wwwRoot = Files.createTempDirectory("webserver-e2e-");
    }

    /**
     * Stops the server and destroys resources belonging to the current test.
     *
     * @throws Exception
     *     if test resources cannot be released.
     */
    @AfterEach
    void destroyTestEnvironment() throws Exception {
        try {
            if (server != null) {
                server.stop();
                server = null;
            }
        } finally {
            if (context != null) {
                context.close();
                context = null;
                page = null;
            }

            deleteDirectory(wwwRoot);
            wwwRoot = null;
        }
    }

    /**
     * Stops the browser and destroys Playwright.
     */
    @AfterAll
    static void stopBrowser() {
        try {
            if (browser != null) {
                browser.close();
                browser = null;
            }
        } finally {
            if (playwright != null) {
                playwright.close();
                playwright = null;
            }
        }
    }

    /**
     * Starts a server using the default handler.
     *
     * @throws ServerException
     *     if the server cannot be started.
     * @throws IOException
     *     if a free local port cannot be allocated.
     */
    protected final void startServer()
        throws ServerException, IOException {

        startServer(null);
    }

    /**
     * Starts a server using the specified request handler.
     *
     * @param handler
     *     the request handler, or {@code null} to use the default handler.
     * @throws ServerException
     *     if the server cannot be started.
     * @throws IOException
     *     if a free local port cannot be allocated.
     */
    protected final void startServer(final Handler handler)
        throws ServerException, IOException {

        if (server != null) {
            throw new IllegalStateException(
                "Server is already running"
            );
        }

        final Options.Builder builder = new Options.Builder()
            .setPort(0)
            .setWwwRoot(wwwRoot.toString());

        if (handler != null) {
            builder.setHandler(handler);
        }

        configure(builder);

        server = Server.start(builder.build());
        port = server.getPort();
    }

    /**
     * Allows a test class to customize server options before the server starts.
     * <p>
     * The default implementation does nothing.
     *
     * @param builder
     *     the server options builder.
     */
    protected void configure(final Options.Builder builder) {
        builder.setReadTimeout(Duration.ofSeconds(1));
    }

    /**
     * Returns the base URL of the currently running server.
     *
     * @return
     *     the base HTTP URL.
     * @throws IllegalStateException
     *     if the server has not been started.
     */
    protected final String getBaseUrl() {
        ensureServerStarted();
        return "http://127.0.0.1:" + port;
    }

    /**
     * Creates an absolute URL for a path on the currently running server.
     *
     * @param path
     *     the request path.
     * @return
     *     the absolute URL.
     */
    protected final String url(final String path) {
        if (path == null || path.isEmpty()) {
            return getBaseUrl() + "/";
        }

        if (path.charAt(0) == '/') {
            return getBaseUrl() + path;
        }

        return getBaseUrl() + "/" + path;
    }

    /**
     * Writes a UTF-8 text file into the temporary WWW root.
     *
     * @param relativePath
     *     the path relative to the WWW root.
     * @param content
     *     the file contents.
     * @return
     *     the created file.
     * @throws IOException
     *     if the file cannot be created.
     */
    protected final Path writeFile(
        final String relativePath,
        final String content
    ) throws IOException {

        final Path file = wwwRoot.resolve(relativePath);
        final Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        return Files.writeString(file, content);
    }

    /**
     * Writes binary data into the temporary WWW root.
     *
     * @param relativePath
     *     the path relative to the WWW root.
     * @param content
     *     the file contents.
     * @return
     *     the created file.
     * @throws IOException
     *     if the file cannot be created.
     */
    protected final Path writeFile(
        final String relativePath,
        final byte[] content
    ) throws IOException {

        final Path file = wwwRoot.resolve(relativePath);
        final Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        return Files.write(file, content);
    }

    /**
     * Ensures that the test server has already been started.
     */
    private void ensureServerStarted() {
        if (server == null) {
            throw new IllegalStateException(
                "Server has not been started"
            );
        }
    }

    /**
     * Recursively removes a directory created for a test.
     *
     * @param directory
     *     the directory.
     * @throws IOException
     *     if the directory cannot be removed.
     */
    private static void deleteDirectory(final Path directory)
        throws IOException {

        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            final Path[] items = paths
                .sorted((left, right) ->
                    right.getNameCount() - left.getNameCount()
                )
                .toArray(Path[]::new);

            for (Path item : items) {
                Files.deleteIfExists(item);
            }
        }
    }

    /**
     * Returns the actual port of the running test server.
     *
     * @return
     *     the server port.
     */
    protected final int getPort() {
        ensureServerStarted();
        return port;
    }

    /**
     * Stops the current test server.
     *
     * @throws ServerException
     *     if the server cannot be stopped.
     */
    protected final void stopServer() throws ServerException {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
