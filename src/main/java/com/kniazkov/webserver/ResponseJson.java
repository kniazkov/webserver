package com.kniazkov.webserver;

import com.kniazkov.json.JsonElement;
import java.nio.charset.StandardCharsets;

public class ResponseJson implements Response {
	public ResponseJson(JsonElement rootElem) {
		this.rootElem = rootElem;
	}
	
	public String getContentType() {
		return "text/javascript";
	}

	public byte[] getData() {
		return rootElem.toString().getBytes(StandardCharsets.UTF_8);
	}

	private final JsonElement rootElem;
}
