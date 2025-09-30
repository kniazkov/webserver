/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple and perfect web server for everyday use.
 */
public final class Server {
	/**
	 * Listener that's listening a socket.
	 */
	private final Listener listener;

	/**
	 * Thread in which the listener is running.
	 */
	private final Thread thread;

	/**
	 * Starts the web server.
	 * @param options Options for starting the server
	 * @param handler Handler that handles requests received from clients (i.e., web pages).
	 * @return An instance of the running server
	 */
	public static Server start(Options options, Handler handler) {
		Server server = new Server(options, handler);
		server.start();
		return server;
	}

	/**
	 * Private constructor.
	 * @param options Options for starting the server
	 * @param handler Handler that handles requests received from clients
	 */
	private Server(Options options, Handler handler) {
		listener = new Listener(options.clone(), handler);
		thread = new Thread(listener);
	}

	/**
	 * Starts the thread in which the listener is running.
	 */
	private void start() {
		thread.start();
	}

	/**
	 * Checks if the server is in a running state.
	 * @return Checking result.
	 */
	public boolean isAlive() {
		return thread.isAlive();
	}

	/**
	 * Stops the web server.
	 */
	public void stop() {
		listener.stop();
	}

	/**
	 * Listener that's listening a socket.
	 */
	private static class Listener implements Runnable {
		/**
		 * Options for starting the server.
		 */
		private final Options options;

		/**
		 * Handler that handles requests received from clients (i.e., web pages).
		 */
		private final Handler handler;

		/**
		 * Flag, as long as it is set, the listener will listen the socket.
		 * As soon as the flag is reset, the server will stop after processing the last request.
		 */
		private volatile boolean work;

		/**
		 * Constructor.
		 * @param options Options for starting the server
		 * @param handler Handler that handles requests received from clients
		 */
		public Listener(final Options options, final Handler handler) {
			this.options = options;
			this.handler = handler;
			work = true;
		}

		/**
		 * Starting point of the listener.
		 */
		public void run() {
			ServerSocket serverSocket = null;
			try {
				serverSocket = new ServerSocket(options.port);
		        ExecutorService pool = Executors.newFixedThreadPool(options.threadCount);
		        while (work)
		        {
		            Socket socket = serverSocket.accept();
		            pool.submit(new Executor(socket, options, handler));
		        }
		        pool.shutdownNow();
				serverSocket.close();
			}
			catch (IOException exception) {
				throw new RuntimeException(exception);
			}
		}

		/**
		 * Stops the server.
		 */
		public void stop() {
			work = false;
		}
	}

	/**
	 * Thread that executes requests received from a client.
	 */
    private static class Executor implements Runnable {
		/**
		 * Socket.
		 */
		private final Socket socket;

		/**
		 * Options.
		 */
		private final Options options;

		/**
		 * Handler that handles requests received from a client.
		 */
		private final Handler handler;

		/**
		 * Constructor.
		 * @param socket Socket
		 * @param options Options
		 * @param handler Handler that handles requests received from a client
		 */
        private Executor(Socket socket, Options options, Handler handler) {
        	this.socket = socket;
        	this.options = options;
        	this.handler = handler;
        }

		/**
		 * Starting point of the executor.
		 * Here the data received from the client is parsed, the handler is called,
		 * and then the resulting data is sent to the client.
		 */
		public void run() {
			try {
				final StreamReader reader = new StreamReader(socket.getInputStream());
				if (options.timeout == 0) {
					processRequest(reader);
				} else {
					while (!socket.isClosed()) {
						socket.setSoTimeout(options.timeout);
						try {
							processRequest(reader);
						} catch (SocketTimeoutException ignored) {
							socket.close();
						}
					}
				}
			} catch (IOException exception) {
				throw new RuntimeException(exception);
			}
		}

		/**
		 * Processes a single HTTP request received from the client.
         *
		 * Reads the request line, headers, and body (if present), constructs a {@link Request}
		 * object, invokes the handler, and writes the resulting {@link Response} or a static file
		 * back to the client.
		 *
		 * @param reader stream Reader used to read the raw request data
		 * @throws IOException If an I/O error occurs while reading the request
		 *  or writing the response
		 */
		private void processRequest(final StreamReader reader) throws IOException {
			final Request request = parseRequest(reader);
			if (request == null) {
				return;
			}
			Response response = handler.handle(request);
			if (response != null) {
				writeResponse(
					"200 OK",
					response.getContentType(),
					response.getData(),
					response.getCookies()
				);
			} else {
				readAndSendLocalFile(request);
			}
			if (request.closeConnection) {
				socket.close();
			}
		}

		/**
		 * Parses an HTTP request received from the client.
		 *
		 * This method reads data from the input stream (request line, headers, and optionally
		 * the body), determines the HTTP method, target address, headers, and fills the
		 * {@link Request} object.
		 * For POST requests, it also extracts form parameters or file data
		 * in case of <code>multipart/form-data</code>.
		 *
		 * @param reader the stream reader used to read client data line by line
		 * @return a {@link Request} object containing the parsed request data
		 * @throws IOException if an error occurs while reading from the socket
		 */
		private Request parseRequest(final StreamReader reader) throws IOException {
			final Request request = new Request();
			int contentLength = 0;
			String boundary = "";

			String line = reader.readLine();
			if (line.isEmpty()) {
				socket.close();
				return null;
			}
			String[] parts = line.split(" ");
			if (parts.length >= 3) {
				String methodStr = parts[0].trim();
				request.address = parts[1].trim();
				request.httpVersion = parts[2].trim();

				if ("GET".equalsIgnoreCase(methodStr)) {
					request.method = Method.GET;
				} else if ("POST".equalsIgnoreCase(methodStr)) {
					request.method = Method.POST;
				}
			}
			line = reader.readLine();
			while (line.length() > 0) {
				int colon = line.indexOf(':');
				if (colon > 0) {
					String name = line.substring(0, colon).trim();
					String value = line.substring(colon + 1).trim();
					request.headers.put(name, value);

					if ("Content-Length".equalsIgnoreCase(name)) {
						try {
							contentLength = Integer.parseInt(value);
						} catch (NumberFormatException ignored) {
							writeResponse("400 Bad Request");
							return null;
						}
					} else if ("Content-Type".equalsIgnoreCase(name) && value.startsWith("multipart/form-data;")) {
						int index = value.indexOf("boundary=");
						if (index != -1) {
							boundary = value.substring(index + 9);
						}
					} else if ("Connection".equalsIgnoreCase(name) && "close".equalsIgnoreCase(value)) {
						request.closeConnection = true;
					} else if ("Cookie".equalsIgnoreCase(name)) {
						String[] pairs = value.split(";");
						for (String pair : pairs) {
							String[] kv = pair.trim().split("=", 2);
							if (kv.length == 2) {
								String cookieName = URLDecoder.decode(kv[0].trim(), "UTF-8");
								String cookieValue = URLDecoder.decode(kv[1].trim(), "UTF-8");
								request.cookies.put(cookieName, cookieValue);
							}
						}
					}
				}
				line = reader.readLine();
			}

			int qIndex = request.address.indexOf('?');
			if (qIndex >= 0) {
				request.path = request.address.substring(0, qIndex);
				request.query = request.address.substring(qIndex + 1);
			} else {
				request.path = request.address;
			}

			if (request.method == Method.UNKNOWN) {
				writeResponse("200 OK", "text/javascript");
			}
			else if (request.method == Method.POST) {
				reader.setLimit(contentLength);
				reader.setBoundary("--" + boundary);
			}

			if (request.method == Method.GET || (request.method == Method.POST && boundary.length() == 0)) {
				String data = "";
				if (request.method == Method.GET)
					data = request.query;
				else
					data = reader.readLine();
				if (data.length() > 0) {
					for (final String item : data.split("&")) {
						if (item != null && !item.equals("")) {
							final String[] pair = item.split("=");
							if (pair.length == 1 || pair.length == 2) {
								final String key = URLDecoder.decode(pair[0], "UTF-8");
								String value = "";
								if (pair.length == 2) {
									value = URLDecoder.decode(pair[1], "UTF-8");
								}
								request.formData.put(key, value);
							}
						}
					}
				}
			}
			else if (boundary.length() > 0) {
				String item = reader.readLine();
				if (!item.equals("--" + boundary)) {
					writeResponse("400 Bad Request");
					return null;
				}
				do {
					String key = "";
					String contentType = "";
					String fileName = "";
					do {
						item = reader.readLine();
						if (item.startsWith("Content-Disposition: form-data")) {
							int nameIndex = item.indexOf("name=\"");
							if (nameIndex >= 0) {
								key = item.substring(nameIndex + 6);
								int closingQuoteIndex = key.indexOf("\"");
								if (closingQuoteIndex < 0) {
									writeResponse("400 Bad Request");
									return null;
								}
								key = key.substring(0, closingQuoteIndex);
							}
							int fileNameIndex = item.indexOf("filename=\"");
							if (fileNameIndex >= 0) {
								fileName = item.substring(fileNameIndex + 10);
								int closingQuoteIndex = fileName.indexOf("\"");
								if (closingQuoteIndex < 0) {
									writeResponse("400 Bad Request");
									return null;
								}
								fileName = fileName.substring(0, closingQuoteIndex);
							}
						}
						else if (item.startsWith("Content-Type:")) {
							contentType = item.substring(14);
						}
					} while (item.length() != 0);
					final byte[] data = reader.readArrayToBoundary();
					final int first = reader.readByte();
					final int second = reader.readByte();
					if (fileName.length() == 0) {
						request.formData.put(key, new String(data, StandardCharsets.UTF_8));
					} else {
						final FileDescriptor file = new FileDescriptor();
						file.name = URLDecoder.decode(fileName, "UTF-8");
						file.contentType = contentType;
						file.data = data;
						request.files.put(key, file);
					}
					if (first != 13 && second != 10) {
						break;
					}
				} while (true);
			}

			return request;
		}

		/**
		 * Reads a static file from the local {@code wwwRoot} folder and sends it to the client.
		 * If the requested path is {@code /}, the default file {@code /index.html} is served.
		 * If the file does not exist, a {@code 404 Not Found} response is returned.
		 * If an I/O error occurs, a {@code 500 Internal Server Error} is returned.
		 *
		 * @param request The parsed HTTP request containing the target address
		 * @throws IOException If an error occurs while reading the file or writing the response
		 */
		private void readAndSendLocalFile(final Request request) throws IOException {
			if (request.address.startsWith("/?")) {
				writeResponse("500 Internal Server Error");
			} else {
				String path = request.address;
				int index = path.indexOf('?');
				if (index >= 0)
					path = path.substring(0, index);
				if (path.equals("/")) {
					path = "/index.html";
				} else {
					path = URLDecoder.decode(path, "UTF-8");
				}
				try {
					File file = new File(options.wwwRoot + path);
					if (file.exists()) {
						byte[] content = Files.readAllBytes(file.toPath());
						String extension = "";
						index = path.lastIndexOf('.');
						if (index > 0)
							extension = path.substring(index + 1).toLowerCase();
						String type = "application/unknown";
						if (extension.length() > 0) {
							switch(extension)
							{
								case "txt":
									type = "text/plain";
									break;
								case "htm":
								case "html":
									type = "text/html";
									break;
								case "css":
									type = "text/css";
									break;
								case "js":
									type = "text/javascript";
									break;
								case "jpg":
								case "jpeg":
								case "png":
								case "gif":
									type = "image/" + extension;
									break;
								default:
									type = "application/" + extension;
							}
						}
						writeResponse("200 OK", type, content);
					}
					else {
						writeResponse("404 Not Found");
					}
				}
				catch (IOException ignored) {
					writeResponse("500 Internal Server Error");
				}
			}
		}

		/**
         * Sends a response to the client without body, without cookies,
         * and without explicitly specified content type.
		 * @param code Response code, for example {@code 404 Not Found}
		 * @throws IOException If there's something wrong with the output stream
		 */
        private void writeResponse(String code) throws IOException {
			writeResponse(code, null, null, null);
		}

		/**
		 * Sends a response to the client without body and without cookies.
		 * @param code Response code, for example {@code 404 Not Found}
		 * @param type Response type, for example, {@code image/jpeg} or {@code text/html}
		 * @throws IOException If there's something wrong with the output stream
		 */
        private void writeResponse(String code, String type) throws IOException {
			writeResponse(code, type, null, null);
		}

		/**
		 * Sends a response to the client. No {@code Set-Cookie} headers are included.
		 * @param code Response code, for example {@code 404 Not Found}
		 * @param type Response type, for example, {@code image/jpeg} or {@code text/html}
		 * @param data Response data, or {@code null} if there is no data
		 * @throws IOException If there's something wrong with the output stream
		 */
        private void writeResponse(String code, String type, byte[] data) throws IOException {
			writeResponse(code, type, data, null);
		}

		/**
		 * Sends a response to the client.
		 * @param code Response code, for example {@code 404 Not Found}
		 * @param type Response type, for example, {@code image/jpeg} or {@code text/html}
		 * @param data Response data, or {@code null} if there is no data
         * @param cookies Map of cookies to set in the response, if empty or {@code null},
         *  no cookies are sent
		 * @throws IOException If there's something wrong with the output stream
		 */
        private void writeResponse(String code, String type, byte[] data,
				Map<String, String> cookies) throws IOException {
			OutputStream stream = socket.getOutputStream();
			if (code != null) {
				if (type == null)
					type = "application/unknown";
				StringBuilder b = new StringBuilder();

				b.append("HTTP/1.1 ");
				b.append(code);
				b.append("\r\n");

				b.append("Access-Control-Allow-Origin: *\r\n");

				b.append("Content-Type: ");
				b.append(type);
				b.append("\r\n");

				b.append("Content-Length: ");
				if (data != null)
					b.append(data.length);
				else
					b.append('0');
				b.append("\r\n");

				if (cookies != null) {
					for (Map.Entry<String, String> entry : cookies.entrySet()) {
						b.append("Set-Cookie: ")
							.append(entry.getKey())
							.append("=")
							.append(entry.getValue())
							.append("; Path=/\r\n");
					}
				}

				if (options.timeout == 0)
					b.append("Connection: close\r\n");
				else
					b.append("Keep-Alive: timeout=")
						.append(Math.max(options.timeout / 1000, 1))
						.append(", max=100\r\n");

				b.append("\r\n");

				stream.write(b.toString().getBytes());
			}
			if (data != null) {
				stream.write(data);
			}
			stream.flush();
        }
    }
}
