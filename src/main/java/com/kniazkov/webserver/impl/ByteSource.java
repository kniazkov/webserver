/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;
import java.io.IOException;

/**
 * Represents a source of bytes.
 */
interface ByteSource {

    /**
     * Reads the next byte from the source.
     *
     * @return
     *     the next byte in the range {@code 0..255}, or {@code -1} if the end
     *     of the source has been reached.
     * @throws IOException
     *     if an I/O error occurs.
     */
    int read() throws ServerException;
}
