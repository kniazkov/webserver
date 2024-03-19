/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

/**
 * The response returned by the handler, that contains nothing.
 */
public final class ResponseNothing implements Response {
	/**
	 * The instance.
	 */
	public static Response INSTANCE = new ResponseNothing();

	/**
	 * Private constructor.
	 */
	private ResponseNothing() {
	}

	public String getContentType() {
		return "text/plain";
	}

	public byte[] getData() {
		return null;
	}
}
