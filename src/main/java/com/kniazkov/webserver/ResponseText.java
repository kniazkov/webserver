/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import java.nio.charset.StandardCharsets;

/**
 * Response returned by the handler, in the form of a plain text.
 */
public final class ResponseText implements Response {
	/**
	 * The text.
	 */
	private final String text;

	/**
	 * Constructor.
	 * @param text The text
	 */
	public ResponseText(String text) {
		this.text = text;
	}

	public String getContentType() {
		return "text/plain";
	}

	public byte[] getData() {
		return text.getBytes(StandardCharsets.UTF_8);
	}
}
