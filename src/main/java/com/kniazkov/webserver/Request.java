/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import java.util.Map;
import java.util.TreeMap;

/**
 * Request received from a client and then parsed.
 */
public final class Request {
    /**
     * The address entered in the address line, or generated by an HTTP request.<br/>
     * Here are some examples:
     * <ul>
     *     <li>
     *         {@code /} - root, meaning there is nothing in the address line after the site name
     *         (i.e. the address line looks like this: {@code http://domain/} or
     *         {@code http://domain:port/})
     *     </li>
     *     <li>
     *         {@code /filename.html} - address contains a file name, i.e. the address line
     *         looks like this: {@code http://domain/filename.html}
     *     </li>
     *     <li>
     *         {@code /?key=value} - address contains form data passed by the GET method
     *     </li>
     *     <li>
     *         {@code /filename.html?key=value} - address contains both the file name and
     *         the form data passed by the GET method
     *     </li>
     * </ul>
     */
    public String address = "";

    /**
     * HTTP request method processed by this server.
     */
    public Method method = Method.UNKNOWN;

    /**
     * Parsed web form data or an empty collection if the request did not contain any form data.
     */
    public Map<String, String> formData = new TreeMap<>();

    /**
     * Parsed web form data containing transferred files using POST method.
     */
    public Map<String, FileDescriptor> files = new TreeMap<>();
}
