/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.e2e;

import com.kniazkov.webserver.Options;

import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for clients that stop consuming responses.
 */
final class SlowConsumerEndToEndTest
    extends EndToEndBaseTest {

    /**
     * Large enough to exceed ordinary TCP buffers when the peer does not read.
     */
    private static final byte[] LARGE_RESPONSE =
        new byte[16 * 1024 * 1024];

    /**
     * Signals that the slow request reached the handler.
     */
    private final CountDownLatch slowRequest =
        new CountDownLatch(1);

    /**
     * Applies a single-worker limit to make worker release observable.
     *
     * @param builder
     *     the options builder.
     */
    @Override
    protected void configure(final Options.Builder builder) {
        super.configure(builder);
        builder
            .setMaxWorkers(1)
            .setWriteTimeout(Duration.ofMillis(200));
    }

    /**
     * Tests that evicting a slow response consumer makes its worker available
     * to a subsequent browser request.
     */
    @Test
    void serverRemainsAvailableAfterSlowConsumer() throws Exception {
        startServer(
            (request, environment) -> {
                if ("/slow".equals(request.getPath().getPath())) {
                    slowRequest.countDown();
                    return environment
                        .getResponseFactory()
                        .fromBytes(LARGE_RESPONSE)
                        .build();
                }

                return environment
                    .getResponseFactory()
                    .fromText("available")
                    .build();
            }
        );

        try (Socket slow = new Socket()) {
            slow.setReceiveBufferSize(1024);
            slow.connect(
                new InetSocketAddress(
                    "127.0.0.1",
                    getPort()
                )
            );
            slow.getOutputStream().write(
                (
                    "GET /slow HTTP/1.1\r\n"
                        + "Host: localhost\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                ).getBytes(StandardCharsets.US_ASCII)
            );
            slow.getOutputStream().flush();

            assertTrue(
                slowRequest.await(2, TimeUnit.SECONDS)
            );

            final var response = page.request().get(
                url("/health"),
                RequestOptions.create().setTimeout(2000)
            );

            assertEquals(200, response.status());
            assertEquals("available", response.text());
        }
    }
}
