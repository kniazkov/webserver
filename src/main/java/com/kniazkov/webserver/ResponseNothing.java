/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

/**
 * The response returned by the handler, that contains nothing.
 */
public final class ResponseNothing implements Response {
	/**
	 * Private constructor.
	 */
	private ResponseNothing() {
	}

	/**
	 * The instance.
	 */
	private static Response instance = null;

	/**
	 * Returns instance of this class.
	 * @return Response that contains nothing.
	 */
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
