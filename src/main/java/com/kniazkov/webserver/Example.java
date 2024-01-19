package com.kniazkov.webserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
                byte[] data = request.get("testFile").getValue().getBytes(StandardCharsets.US_ASCII);
                try (FileOutputStream outputStream = new FileOutputStream("X:\\uploaded")) {
                    outputStream.write(data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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
