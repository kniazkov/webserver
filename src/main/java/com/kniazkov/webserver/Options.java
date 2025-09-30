/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

/**
 * Some options for starting the server.
 */
public final class Options implements Cloneable {
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

    /**
     * Socket read timeout in milliseconds.
     * Defines how long the server will wait for client data before closing the connection
	 * due to inactivity.
     */
	public int timeout = 0;

	/**
	 * Creates and returns a copy of this {@code Options} instance.
	 *
	 * @return a shallow copy of this object
	 */
	@Override
	public Options clone() {
		Options o = new Options();
		o.port = port;
		o.wwwRoot = wwwRoot;
		o.threadCount = threadCount;
		o.timeout = timeout;
		return o;
	}
}
