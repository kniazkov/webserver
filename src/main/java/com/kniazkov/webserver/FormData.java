package com.kniazkov.webserver;

public class FormData {
	public FormData(String value) {
		this.value = value;
	}
	
	public FormData(String fileName, String value) {
		this.fileName = fileName;
		this.value = value;
	}
	
	public boolean isFile() {
		return fileName != null;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public String getValue() {
		return value;
	}
	
	private String fileName;
	private String value;
}
