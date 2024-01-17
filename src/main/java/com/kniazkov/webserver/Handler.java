package com.kniazkov.webserver;

import java.util.Map;

public interface Handler {
	Response handle(Map<String, FormData> request);
	Response handle(String address);
}
