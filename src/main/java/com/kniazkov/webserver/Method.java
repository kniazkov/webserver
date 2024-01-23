/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

/**
 * HTTP request methods processed by this server.
 */
public enum Method {
    /**
     * Neither GET nor POST.
     */
    UNKNOWN,

    /**
     * GET is used to request data from a specified resource.
     */
    GET,

    /**
     * POST is used to send data to a server to create/update a resource, including binary data.
     */
    POST
}
