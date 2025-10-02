/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.webserver.example;

import com.kniazkov.json.JsonObject;
import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Request;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.ResponseJson;
import com.kniazkov.webserver.ResponseNothing;
import com.kniazkov.webserver.Server;

/**
 * Web server that returns some JSON document.
 * How to use:
 *   1. Run the program;
 *   2. Open your browser and type "<a href="http://localhost:8000">...</a>" in the address bar.
 */
public class Counter {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        Options options = new Options();
        Handler handler = new Handler() {
            int count = 0;

            @Override
            public Response handle(Request request) {
                if (request.address.equals("/")) {
                    JsonObject json = new JsonObject();
                    json.addNumber("count", count);
                    count++;
                    return new ResponseJson(json);
                }
                return ResponseNothing.INSTANCE;
            }
        };

        Server.start(options, handler);
    }
}
