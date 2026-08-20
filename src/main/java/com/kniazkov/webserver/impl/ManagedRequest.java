/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

package com.kniazkov.webserver.impl;

import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.ServerException;

/**
 * Request that owns upload storage requiring deterministic cleanup.
 */
interface ManagedRequest extends Request {

    /**
     * Releases request upload storage.
     *
     * @throws ServerException
     *     if cleanup fails.
     */
    void close() throws ServerException;
}
