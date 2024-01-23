/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

public final class ResponseNothing implements Response {
	private ResponseNothing() {
	}
	
	private static Response instance = null;
	
	public static Response getInstance() {
		if (instance == null)
			instance = new ResponseNothing();
		return instance;
	}
	
	public String getContentType() {
		return "text/plain";
	}

	public byte[] getData() {
		return null;
	}
}
