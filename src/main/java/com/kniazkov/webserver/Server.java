package com.kniazkov.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Server {
	public static Server start(Options opt, Handler handler) {
		Server server = new Server(opt, handler);
		server.start();
		return server;
	}
	
	private Server(Options opt, Handler handler) {
		listener = new Listener(opt, handler);
		thread = new Thread(listener);
	}
	
	private void start() {
		thread.start();
	}
	
	public boolean isAlive() {
		return thread.isAlive();
	}
	
	public void stop() {
		listener.stop();
	}
	
	private final Listener listener;
	private final Thread thread;
	
	private static class Listener implements Runnable {
		private final Options opt;
		private final Handler handler;
		private volatile boolean work;
	
		public Listener(Options opt, Handler handler) {
			this.opt = opt;
			this.handler = handler;
			work = true;
		}

		public void run() {
			ServerSocket ss = null;
			try {
		        ss = new ServerSocket(opt.port);
		        ExecutorService pool = Executors.newFixedThreadPool(opt.threadCount);
		        while (work)
		        {
		            Socket s = ss.accept();
		            pool.submit(new Processor(s, opt.wwwRoot, handler));
		        }
		        pool.shutdownNow();
			}
			catch (Throwable e) {
				System.err.println("HTTP server failed: " + e.toString());
			}

			try {
				if (ss != null)
					ss.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void stop() {
			work = false;
		}
	}
	
    private static class Processor implements Runnable {

        private Processor(Socket socket, String wwwRoot, Handler handler) {
        	this.socket = socket;
        	this.wwwRoot = wwwRoot;
        	this.handler = handler;
        }

        private final Socket socket;
        private final String wwwRoot;
        private final Handler handler;

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

		/*
        public void run() {
            try {

            	writeResponse("102 Processing", null, null);
            	
                BufferedReader buff = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                while(true)
                {
                    String s = buff.readLine();
                    if(s == null || s.trim().length() == 0) {
                        break;
                    }
                    if (s.startsWith("GET")) {
                    	int n = s.indexOf("HTTP");
                    	if (n != -1)
                    		address = s.substring(4,  n - 1);
                    	method = Method.GET;
                    }
                    else if (s.startsWith("POST")) {
                    	int n = s.indexOf("HTTP");
                    	if (n != -1)
                    		address = s.substring(5,  n - 1);
                    	method = Method.POST; 
                    }
                    else if (s.startsWith("Content-Length:")) {
                    	try {
                    		contentLength = Integer.parseInt(s.substring(16));
                    	}
                    	catch(NumberFormatException e) {
                    		contentLength = 0;
                    	}
                    }
                    else if (s.startsWith("Content-Type: multipart/form-data;")) {
                    	int n = s.indexOf("boundary=");
                    	if (n != -1)
                    		boundary = s.substring(n + 9);
                    }
                }
                if (method == Method.POST && contentLength > 0) {
                	Thread.sleep(250);
                	char[] tmp = new char[contentLength];
                	char[] data = new char[contentLength];
                	int offset = 0;
                	int readCnt;
                	while (offset < contentLength) {
                		readCnt = buff.read(tmp);
                		if (readCnt == -1)
                			break;
                		System.arraycopy(tmp, 0, data, offset, readCnt);
                		offset += readCnt;
                    	writeResponse("202 Accepted", null, null);
                	}
            		postData = String.valueOf(data);
                }
                
                if (method == Method.UNKNOWN) {
            		writeResponse("200 OK", "text/javascript", null);
                }
                else if ((address != null && address.startsWith("/?")) || (method == Method.POST && boundary == null)) {
                	TreeMap<String, FormData> map = new TreeMap<String, FormData>();
                	String request;
                	if (method == Method.GET)
                		request = address.substring(2);
                	else
                		request = postData;
                	String[] split = request.split("&");
                	for(String pair : split) {
                		if (pair != null && !pair.equals("")) {
                    		String[] keyVal = pair.split("=");
                    		if (keyVal.length == 2) {
                    			String key = URLDecoder.decode(keyVal[0], "UTF-8");
                    			FormData value = new FormData(URLDecoder.decode(keyVal[1], "UTF-8"));
                    			map.put(key, value);
                    		}
                		}
                	}
                	Response response = handler.handle(map);
                	if (response != null)
						writeResponse("200 OK", response.getContentType(), response.getData());
                	else
                		writeResponse("500 Internal Server Error", null, null);
                }
                else if (method == Method.POST && boundary != null) {
                	TreeMap<String, FormData> map = new TreeMap<String, FormData>();
                	String[] split = postData.split("--" + boundary);
                	for(String part : split) {
                		int i = part.indexOf("Content-Disposition");
                		if (i > -1 && i < 10) {
                			int j = part.indexOf("\r\n\r\n");
                			if (j > -1) {
            					String fileName = null;
                				String value = part.substring(j + 4, part.length() - 2);
                				String header = part.substring(0, j);
                				int k = header.indexOf("name=\"");
                				if (k > -1)
                				{
                					header = header.substring(k + 6);
                					k = header.indexOf('"');
                					String key = header.substring(0, k);
                					header = header.substring(k + 1);
                					k = header.indexOf("filename=\"");
                					if (k > -1)
                					{
                						header = header.substring(k + 10);
                						k = header.indexOf('"');
                						fileName = header.substring(0, k);
                					}
                					map.put(key, new FormData(fileName, value));
                				}
                			}
                		}
                	}
                	Response response = handler.handle(map);
                	if (response != null)
                		writeResponse("200 OK", response.getContentType(), response.getData());
                	else
                		writeResponse("500 Internal Server Error", null, null);
                }
                else {
                	int paramsIdx = address.indexOf('?');
                	if (paramsIdx >= 0)
                		address = address.substring(0, paramsIdx);
                	if (address.equals("/"))
                		address = "/index.html";
                	else
                		address = URLDecoder.decode(address, "UTF-8");
                	Response response = handler.handle(address);
                	if (response != null) {
                		writeResponse("200 OK", response.getContentType(), response.getData());
                	}
                	else {
	                	String path = wwwRoot + address;
	                	try {
	                		File file = new File(path);
	                		if (file.exists())
	                		{
	                			byte[] data = Files.readAllBytes(file.toPath());
	                			String ext = null;
	                			int i = address.lastIndexOf('.');
	                			if (i > 0)
	                				ext = address.substring(i + 1).toLowerCase();
	                			String type = "application/unknown";
	                			if (ext != null) {
	                				switch(ext)
	                				{
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
	                					type = "image/" + ext;
	                					break;
	                				default:
	                					type = "application/" + ext;
	                				}
	                			}
	                    		writeResponse("200 OK", type, data);
	                		}
	                		else
	                    		writeResponse("404 Not Found", null, null);
	                	}
	                	catch (Throwable t) {
	                		writeResponse("500 Internal Server Error", null, null);
	                	}
                	}
                }
            }
            catch (Throwable t) {
            	if (!(t instanceof java.net.SocketException))
            		t.printStackTrace();
            }
            finally {
                try {
                    socket.close();
                }
                catch (Throwable t) {
                	t.printStackTrace();
                }
            }
        }
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
