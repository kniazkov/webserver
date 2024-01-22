package com.kniazkov.webserver;

import java.util.Map;
import java.util.TreeMap;

public class Request {
    public String address = "";

    public Method method = Method.UNKNOWN;

    public Map<String, String> formData = new TreeMap<>();
}
