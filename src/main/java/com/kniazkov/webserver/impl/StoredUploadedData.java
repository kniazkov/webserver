/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.ServerException;
import com.kniazkov.webserver.UploadedData;

/**
 * Uploaded data owned by a parsed request.
 */
interface StoredUploadedData extends UploadedData {

    /**
     * Creates a view of a range within this data.
     *
     * @param offset
     *     the range offset.
     * @param length
     *     the range length.
     * @return
     *     the range view.
     */
    StoredUploadedData slice(long offset, long length);

    /**
     * Releases the underlying storage.
     *
     * @throws ServerException
     *     if the storage cannot be released.
     */
    void close() throws ServerException;
}
