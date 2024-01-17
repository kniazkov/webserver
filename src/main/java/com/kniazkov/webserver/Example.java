package com.kniazkov.webserver;

import java.util.Map;

public class Example {
    public static void main(String[] args) {
        Options opt = new Options();
        opt.wwwRoot = "./example";

        Handler handler = new Handler() {
            @Override
            public Response handle(Map<String, FormData> request) {
                return ResponseNothing.getInstance();
            }

            @Override
            public Response handle(String address) {
                return null;
            }
        };

        Server.start(opt, handler);
    }
}
