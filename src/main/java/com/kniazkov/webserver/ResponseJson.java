/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import com.kniazkov.json.JsonElement;
import java.nio.charset.StandardCharsets;

public final class ResponseJson implements Response {
	private final JsonElement rootElem;

	public ResponseJson(JsonElement rootElem) {
		this.rootElem = rootElem;
	}
	
	public String getContentType() {
		return "text/javascript";
	}

	public byte[] getData() {
		return rootElem.toString().getBytes(StandardCharsets.UTF_8);
	}
}
