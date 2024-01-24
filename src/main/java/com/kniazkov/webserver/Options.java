/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

/**
 * Some options for starting the server.
 */
public final class Options {
	/**
	 * Port number.
	 * Typically, the numbers used for the HTTP protocol are 80, 8000, 8080.
	 */
	public int port = 8000;

	/**
	 * A folder in which web page files, such as 'index.html', are located.
	 */
	public String wwwRoot = "./www";

	/**
	 * The number of simultaneous requests that the server can handle.
	 */
	public int threadCount = 16;
}
