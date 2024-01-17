package com.kniazkov.webserver;

public class Options {
	public int port;
	public String wwwRoot;
	public int threadCount;
	
	public Options() {
		port = 8000;
		wwwRoot = "./www";
		threadCount = 16;
	}
}
