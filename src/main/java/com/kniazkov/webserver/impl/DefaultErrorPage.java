/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ErrorPage;

/**
 * Default implementation of an HTTP error page.
 */
public final class DefaultErrorPage implements ErrorPage {

    /**
     * The singleton instance.
     */
    private static final ErrorPage INSTANCE = new DefaultErrorPage();

    /**
     * Prevents external instantiation.
     */
    private DefaultErrorPage() {
    }

    /**
     * Returns the default error page.
     *
     * @return
     *     the default error page.
     */
    public static ErrorPage getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String create(
        final int code,
        final String reason,
        final String message
    ) {
        final String safeReason = escape(reason);
        final String safeMessage = escape(message);

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%d %s</title>
                <style>
                    html, body {
                        height: 100%%;
                        margin: 0;
                    }
                    body {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: #f5f5f5;
                        color: #222;
                        font-family: sans-serif;
                    }
                    main {
                        max-width: 600px;
                        padding: 40px;
                        text-align: center;
                    }
                    h1 {
                        margin: 0;
                        font-size: 72px;
                    }
                    h2 {
                        margin: 8px 0 24px;
                        font-size: 24px;
                        font-weight: normal;
                    }
                    p {
                        margin: 0;
                        color: #666;
                        line-height: 1.5;
                    }
                </style>
            </head>
            <body>
                <main>
                    <h1>%d</h1>
                    <h2>%s</h2>
                    <p>%s</p>
                </main>
            </body>
            </html>
            """.formatted(
            code,
            safeReason,
            code,
            safeReason,
            safeMessage
        );
    }

    /**
     * Escapes text for insertion into HTML.
     *
     * @param value
     *     the source text.
     * @return
     *     the escaped text.
     */
    private static String escape(final String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
