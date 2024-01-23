/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import java.util.Map;
import java.util.TreeMap;

public final class Request {
    public String address = "";

    public Method method = Method.UNKNOWN;

    public Map<String, String> formData = new TreeMap<>();

    public Map<String, File> files = new TreeMap<>();

    static class File {
        public String name;

        public String contentType;

        public byte[] data;
    }
}
