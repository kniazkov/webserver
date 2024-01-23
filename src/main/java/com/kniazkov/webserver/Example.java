package com.kniazkov.webserver;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class Example {
    public static void main(String[] args) {

        Options options = new Options();
        options.wwwRoot = "./example";

        Handler handler = new MyHandler();

        Server.start(options, handler);
    }

    private static class MyHandler implements Handler {
        private int count = 0;
        private final Map<String, Request.File> uploadedFiles = new TreeMap<>();

        @Override
        public Response handle(final Request request) {
            if (request.address.startsWith("/upload")) {
                final StringBuilder html = new StringBuilder();
                html.append("<html><body>");
                final Request.File file = request.files.get("testFile");
                boolean uploaded = false;
                if (file != null) {
                    int index = file.name.lastIndexOf('.');
                    if (index >= 0) {
                        final String extension = file.name.substring(index + 1);
                        final String newFileName = "" + count + extension;
                        count++;
                        uploadedFiles.put(newFileName, file);
                        uploaded = true;
                        html.append("<img src=\"/img/").append(newFileName).append("\"/>");
                        html.append("<p>Uploaded ").append(file.data.length).append(" bytes.</p>");
                    }
                }
                if (!uploaded) {
                    html.append("<p>No file has been uploaded.</p>");
                }
                html.append("<p><a href=\"index.html\">Go back</a></p>");
                html.append("</body></html>");
                return new Response() {
                    @Override
                    public String getContentType() {
                        return "text/html";
                    }

                    @Override
                    public byte[] getData() {
                        return html.toString().getBytes(StandardCharsets.UTF_8);
                    }
                };
            } else if (request.address.startsWith("/img/")) {
                final String fileName = request.address.substring(5);
                final Request.File file = uploadedFiles.get(fileName);
                if (file != null) {
                    return new Response() {
                        @Override
                        public String getContentType() {
                            return file.contentType;
                        }

                        @Override
                        public byte[] getData() {
                            return file.data;
                        }
                    };
                }
            }
            return null;
        }
    }
}
