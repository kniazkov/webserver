package com.kniazkov.webserver;

import java.nio.charset.StandardCharsets;

public final class ResponseText implements Response {
	private final String text;

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
