package com.kniazkov.webserver;

import java.io.FileOutputStream;
import java.io.IOException;

public class Example {
    public static void main(String[] args) {

        Options options = new Options();
        options.wwwRoot = "./example";

        Handler handler = new MyHandler();

        Server.start(options, handler);
    }

    private static class MyHandler implements Handler {
        @Override
        public Response handle(Request request) {
            if (request.files.containsKey("testFile")) {
                try {
                    FileOutputStream outputStream = new FileOutputStream("X:/uploaded.jpeg");
                    outputStream.write(request.files.get("testFile").data);
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
            return null;
        }
    }
}
