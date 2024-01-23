/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

public interface Handler {
	Response handle(Request request);
}
