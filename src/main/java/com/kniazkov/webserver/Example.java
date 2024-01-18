package com.kniazkov.webserver;

import java.util.Map;

public class Example {
    public static void main(String[] args) {
        Options opt = new Options(){
            @Override
            public String getWwwRoot() {
                return "./example";
            }
        };

        Handler handler = new Handler() {
            @Override
            public Response handle(Map<String, FormData> request) {
                return ResponseNothing.getInstance();
            }

            @Override
            public Response handle(String address) {
                if (address.equals("/test")) {
                    return new Response() {
                        @Override
                        public String getContentType() {
                            return "text/html";
                        }

                        @Override
                        public byte[] getData() {
                            String html = "<html><body><h1>Test page</h1><p>It works.</p></body></html>";
                            return html.getBytes();
                        }
                    };
                }
                return null;
            }
        };

        Server.start(opt, handler);
    }
}
