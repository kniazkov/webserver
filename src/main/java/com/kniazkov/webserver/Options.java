/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonException;
import java.io.File;

/**
 * Some options for starting the server.
 */
public final class Options implements Cloneable {
	/**
	 * Port number.
	 * Typically, the numbers used for the HTTP protocol are 80, 8000, 8080.
	 */
	public int port = 8000;

	/**
	 * A folder in which web page files, such as 'index.html', are located.
	 */
	public String wwwRoot = "./www";

	/**
	 * The number of simultaneous requests that the server can handle.
	 */
	public int threadCount = 16;

    /**
     * Socket read timeout in milliseconds.
     * Defines how long the server will wait for client data before closing the connection
	 * due to inactivity.
     */
	public int timeout = 0;

    /**
     * Path to the keystore file (e.g. {@code keystore.jks}) used for HTTPS connections.
     * If {@code null}, the server will start in plain HTTP mode.
	 * Example of how to get self-signed {@code keystore.jks} for test purposes:
	 * <code>
	 *     keytool -genkeypair -alias testserver -keyalg RSA -keysize 2048 -validity 365 -keystore keystore.jks -storepass changeit
	 * </code>
     */
    public String certificate = null;

    /**
     * Password for the keystore file.
     * Ignored if {@link #certificate} is {@code null}.
     */
    public String keystorePassword = null;

    /**
     * Password for the private key inside the keystore.
     * If {@code null}, {@link #keystorePassword} is used instead.
     */
    public String keyPassword = null;

	/**
	 * Creates and returns a copy of this {@code Options} instance.
	 *
	 * @return A shallow copy of this object
	 */
	@Override
	public Options clone() {
		Options o = new Options();
		o.port = port;
		o.wwwRoot = wwwRoot;
		o.threadCount = threadCount;
		o.timeout = timeout;
        o.certificate = certificate;
        o.keystorePassword = keystorePassword;
        o.keyPassword = keyPassword;
		return o;
	}

    /**
     * Loads server options from a JSON configuration file.
     *
     * @param file The JSON file containing the serialized {@link Options} data
     * @return A new {@code Options} instance populated from the file
     * @throws JsonException If the file cannot be parsed or does not match the expected format
     */
	public static Options loadFromFile(final File file) throws JsonException {
		return Json.parse(file, Options.class);
	}

   /**
     * Loads server options from a JSON configuration file, or returns default values if
     * the file cannot be parsed.
     *
     * @param file The JSON file containing the serialized {@link Options} data
     * @return An {@code Options} instance loaded from the file, or a new instance with
     *  default values if parsing fails
     */
	public static Options loadFromFileOrDefault(final File file) {
		try {
			return loadFromFile(file);
		} catch (JsonException ignored) {
			return new Options();
		}
	}
}
