/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

public interface Response {
	String getContentType();

	byte[] getData();
}
