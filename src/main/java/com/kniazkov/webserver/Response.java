/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import java.util.Map;

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
	 * @return Response data or {@code null} if the response has no data
	 */
	byte[] getData();

    /**
     * Returns the cookies to be set in the response.
     * @return Map of cookies or an empty map or {@code null} if none
     */
    default Map<String, String> getCookies() {
        return null;
    }
}
