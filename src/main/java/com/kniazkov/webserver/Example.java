package com.kniazkov.webserver;

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
            return null;
        }
    }
}
