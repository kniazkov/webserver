/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import com.kniazkov.json.JsonElement;
import java.nio.charset.StandardCharsets;

/**
 * Response returned by the handler, in the form of a JSON document.
 */
public final class ResponseJson implements Response {
	/**
	 * Root element of the JSON tree.
	 */
	private final JsonElement rootElem;

	/**
	 * Constructor.
	 * @param rootElem Root element of the JSON tree
	 */
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
