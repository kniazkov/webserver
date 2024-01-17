package com.kniazkov.webserver;

public interface Options {
	default int getPort() {
		return 8000;
	}

	default String getWwwRoot() {
		return "./www";
	}

	default int getThreadCount() {
		return 16;
	}
}
