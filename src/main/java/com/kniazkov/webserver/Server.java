/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
		listener = new Listener(options, handler);
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
		            pool.submit(new Executor(socket, options.wwwRoot, handler));
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
		 * Folder in which web page files, such as 'index.html', are located.
		 */
		private final String wwwRoot;

		/**
		 * Handler that handles requests received from a client.
		 */
		private final Handler handler;

		/**
		 * Constructor.
		 * @param socket Socket
		 * @param wwwRoot Folder in which web page files, such as 'index.html', are located
		 * @param handler Handler that handles requests received from a client
		 */
        private Executor(Socket socket, String wwwRoot, Handler handler) {
        	this.socket = socket;
        	this.wwwRoot = wwwRoot;
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
				final Request request = new Request();
				int contentLength = 0;
				String boundary = "";

				String line = reader.readLine();
				while (line.length() > 0) {
					if (line.startsWith("GET")) {
						int index = line.indexOf("HTTP");
						if (index != -1)
							request.address = line.substring(4,  index - 1);
						request.method = Method.GET;
					}
					else if (line.startsWith("POST")) {
						int index = line.indexOf("HTTP");
						if (index != -1)
							request.address = line.substring(5,  index - 1);
						request.method = Method.POST;
					}
					else if (line.startsWith("Content-Length:")) {
						try {
							contentLength = Integer.parseInt(line.substring(16));
						}
						catch(NumberFormatException ignored) {
							writeResponse("500 Internal Server Error", null, null);
							return;
						}
					}
					else if (line.startsWith("Content-Type: multipart/form-data;")) {
						int index = line.indexOf("boundary=");
						if (index != -1)
							boundary = line.substring(index + 9);
					}
					line = reader.readLine();
				}

				if (request.method == Method.UNKNOWN) {
					writeResponse("200 OK", "text/javascript", null);
				}
				else if (request.method == Method.POST) {
					reader.setLimit(contentLength);
					reader.setBoundary("--" + boundary);
				}

				if (request.method == Method.GET || (request.method == Method.POST && boundary.length() == 0)) {
					String data = "";
					if (request.method == Method.GET) {
						final int index = request.address.indexOf('?');
						if (index >= 0) {
							data = request.address.substring(index + 1);
						}
					}
					else {
						data = reader.readLine();
					}
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
					try {
						Thread.sleep(250);
					} catch (InterruptedException ignored) {
					}
					String item = reader.readLine();
					if (!item.equals("--" + boundary)) {
						writeResponse("400 Bad Request", null, null);
						return;
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
										writeResponse("400 Bad Request", null, null);
										return;
									}
									key = key.substring(0, closingQuoteIndex);
								}
								int fileNameIndex = item.indexOf("filename=\"");
								if (fileNameIndex >= 0) {
									fileName = item.substring(fileNameIndex + 10);
									int closingQuoteIndex = fileName.indexOf("\"");
									if (closingQuoteIndex < 0) {
										writeResponse("400 Bad Request", null, null);
										return;
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

				Response response = handler.handle(request);
				if (response != null) {
					writeResponse("200 OK", response.getContentType(), response.getData());
				} else {
					if (request.address.startsWith("/?")) {
						writeResponse("500 Internal Server Error", null, null);
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
							File file = new File(wwwRoot + path);
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
								writeResponse("404 Not Found", null, null);
							}
						}
						catch (IOException ignored) {
							writeResponse("500 Internal Server Error", null, null);
						}
					}
				}
			} catch (IOException exception) {
				throw new RuntimeException(exception);
			}
		}

		/**
		 * Sends a response to the client.
		 * @param code Response code, for example {@code 404 Not Found}
		 * @param type Response type, for example, {@code image/jpeg} or {@code text/html}
		 * @param data Response data
		 * @throws IOException If there's something wrong with the output stream
		 */
        private void writeResponse(String code, String type, byte[] data) throws IOException {
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

				b.append("Connection: close\r\n");

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
