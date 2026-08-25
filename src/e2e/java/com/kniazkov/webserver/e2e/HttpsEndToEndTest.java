/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.SslOptions;
import com.kniazkov.webserver.SslProtocol;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests for HTTPS support.
 */
final class HttpsEndToEndTest extends EndToEndBaseTest {

    /**
     * Configures HTTPS for the test server.
     *
     * @param builder
     *     the server options builder.
     */
    @Override
    protected void configure(final Options.Builder builder) {
        try {
            final URL resource = Objects.requireNonNull(
                getClass().getResource("/test-certificate.p12"),
                "Test certificate is missing"
            );

            final char[] password = "test-password".toCharArray();
            final SslOptions sslOptions;
            try {
                sslOptions = new SslOptions.Builder()
                    .setKeyStoreFile(
                        Path.of(resource.toURI()).toFile()
                    )
                    .setPassword(password)
                    .setEnabledProtocols(
                        SslProtocol.TLS_1_2,
                        SslProtocol.TLS_1_3
                    )
                    .setCipherSuites(
                        "TLS_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
                    )
                    .build();
            } finally {
                Arrays.fill(password, '\0');
            }

            builder
                .setSslOptions(sslOptions)
                .setBindAddress(
                    InetAddress.getByName("127.0.0.1")
                )
                .setBacklog(32);
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Cannot configure test SSL certificate",
                exception
            );
        }
        super.configure(builder);
    }

    /**
     * Tests opening a static page over HTTPS.
     */
    @Test
    void httpsWorks() throws Exception {
        writeFile(
            "index.html",
            "<html><body><h1>HTTPS works</h1></body></html>"
        );

        startServer();

        assertEquals(
            InetAddress.getByName("127.0.0.1"),
            getBindAddress()
        );

        page.navigate(
            "https://127.0.0.1:"
                + getPort()
                + "/index.html"
        );

        assertThat(page.locator("h1"))
            .hasText("HTTPS works");
    }
}
