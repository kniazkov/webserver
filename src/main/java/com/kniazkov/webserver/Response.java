/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

/**
 * Response returned by a handler to be sent to client.
 */
public interface Response {
	/**
	 * Returns the content type, for example, {@code image/jpeg} or {@code text/html}.
	 * @return Content type
	 */
	String getContentType();

	/**
	 * Returns the response data as an array of bytes.
	 * @return Response data or {@code null} if the response has no data.
	 */
	byte[] getData();
}
