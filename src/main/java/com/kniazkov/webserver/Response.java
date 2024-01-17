package com.kniazkov.webserver;

public interface Response {
	String getContentType();
	byte[] getData();
}
