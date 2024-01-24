/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

/**
 * Web server payload interface to be implemented by the library user.
 */
public interface Handler {
	/**
	 * Handles requests received from clients (i.e., web pages).
	 *
	 * The behavior of the server depends on the request data and the response
	 * received as a result of this method.
	 * If the result is not {@code null}, it will be sent to the client as is with HTTP status
	 * code 200 (Ok).
	 * If the result is {@code null} and the request address contains a filename,
	 * then the web server will attempt to read that file and return its content to the client
	 * (with code 200). If the file is not found, the server will return code 404.
	 * Thus, if a handler is implemented that always returns {@code null}, the web server will
	 * simply transfer files from a specific folder.
	 * If the result is {@code null} and the request address does not contain a filename,
	 * but only form data, then the server will return a code 500 (internal server error).
	 *
	 * @param request Request
	 * @return Response or {@code null} if the request cannot be handled
	 */
	Response handle(Request request);
}
