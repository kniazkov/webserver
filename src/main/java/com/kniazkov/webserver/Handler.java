package com.kniazkov.webserver;

public interface Handler {
	Response handle(Request request);
}
