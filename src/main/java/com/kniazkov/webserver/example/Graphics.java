/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.webserver.example;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.Options;
import com.kniazkov.webserver.Response;
import com.kniazkov.webserver.Server;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * HTTPS server that generates some graphics and outputs them in PNG format.
 * How to use:
 *   1. Generate SSL certificate:
 *      keytool -genkeypair -alias testserver -keyalg RSA -keysize 2048 -validity 365 -keystore keystore.jks -storepass changeit
 *   2. Run the program;
 *   3. Open your browser and type "<a href="https://localhost">...</a>" in the address bar.
 */
public class Graphics {
    /**
     * Starting point.
     * @param args Program arguments
     */
    public static void main(String[] args) {
        final Options options = new Options();
        options.timeout = 5000;
        options.port = 443;
        options.certificate = "keystore.jks";
        options.keystorePassword = "changeit";
        options.keyPassword = "changeit";
        final BufferedImage image = new BufferedImage(
                500,
                500,
                BufferedImage.TYPE_3BYTE_BGR
        );
        final Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.RED);
        graphics.drawOval(50 , 50, 400, 400);
        graphics.drawOval(120 , 100, 100, 100);
        graphics.drawOval(280 , 100, 100, 100);
        graphics.drawArc(100, 100, 300, 300, 180, 180);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", stream);
        } catch (IOException ignored) {
        }
        final byte[] bytes = stream.toByteArray();
        final Handler handler = request -> new Response() {
            @Override
            public String getContentType() {
                return "image/png";
            }

            @Override
            public byte[] getData() {
                return bytes;
            }
        };

        Server.start(options, handler);
    }
}
