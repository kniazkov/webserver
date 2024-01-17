package com.kniazkov.webserver;

public final class ResponseText implements Response {
	public ResponseText(String text) {
		this.text = text;
	}

	public String getContentType() {
		return "text/plain";
	}

	public byte[] getData() {
		return text.getBytes();
	}

	private String text;
}
