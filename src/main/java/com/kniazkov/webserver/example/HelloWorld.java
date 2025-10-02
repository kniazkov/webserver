/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.webserver.example;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseText;
import com.kniazkov.webserver.Server;

/**
 * Web server that returns text document with "hello world" content.
 * How to use:
 *   1. Run the program;
 *   2. Open your browser and type "<a href="http://localhost:8000">...</a>" in the address bar.
 */
public class HelloWorld {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        Options options = new Options();
        Handler handler = new Handler() {

            @Override
            public Response handle(Request request) {
                return new ResponseText("hello world");
            }
        };

        Server.start(options, handler);
    }
}
