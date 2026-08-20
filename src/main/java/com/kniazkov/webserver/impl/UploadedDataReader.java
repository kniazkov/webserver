/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;

/**
 * Selects storage for an uploaded request body.
 */
final class UploadedDataReader {

    /**
     * Prevents instantiation.
     */
    private UploadedDataReader() {
    }

    /**
     * Reads an exact request body using memory or temporary storage.
     *
     * @param source
     *     the source.
     * @param length
     *     the declared content length.
     * @param memoryLimit
     *     the maximum body size held in memory.
     * @return
     *     the uploaded request data.
     * @throws ServerException
     *     if the request body cannot be read.
     */
    static StoredUploadedData read(
        final ByteSource source,
        final long length,
        final long memoryLimit
    ) throws ServerException {
        if (
            length <= memoryLimit
                && length <= Integer.MAX_VALUE
        ) {
            return MemoryUploadedData.read(
                source,
                (int) length
            );
        }

        return TemporaryFileUploadedData.read(source, length);
    }
}
