/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.plugin.swing.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * HTTP-based MCP server implementation using JDK's built-in HTTP server.
 * This allows any MCP client to connect to a running Codion application via HTTP.
 */
final class SwingMcpHttpServer {

	private static final Logger LOG = LoggerFactory.getLogger(SwingMcpHttpServer.class);

	private static final String CONTENT_TYPE = "Content-Type";
	private static final String APPLICATION_JSON = "application/json";
	private static final String POST = "POST";
	private static final String GET = "GET";

	/**
	 * Tool specification for HTTP MCP.
	 */
	record HttpTool(String name, String description, String inputSchema, ToolHandler handler) {}

	/**
	 * Simple tool handler that returns a result object.
	 */
	@FunctionalInterface
	interface ToolHandler {

		/**
		 * Handle a tool invocation.
		 * @param arguments the tool arguments
		 * @return the result object (typically a string or structured data)
		 * @throws Exception if the tool execution fails
		 */
		Object handle(Map<String, Object> arguments) throws Exception;
	}

	private final int port;
	private final ObjectMapper objectMapper;
	private final Map<String, HttpTool> tools;
	private final String serverName;
	private final String serverVersion;

	private HttpServer server;

	SwingMcpHttpServer(int port, String name, String version) {
		this.port = port;
		this.serverName = name;
		this.serverVersion = version;
		this.objectMapper = new ObjectMapper();
		this.tools = new HashMap<>();
	}

	/**
	 * Add a tool to the server.
	 * @param tool the tool to add
	 * @return this server instance
	 */
	void addTool(HttpTool tool) {
		tools.put(tool.name, tool);
	}

	/**
	 * Start the HTTP server.
	 * @throws IOException if the server cannot be started
	 */
	void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.setExecutor(Executors.newCachedThreadPool(runnable -> {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);

			return thread;
		}));

		// Register endpoints
		server.createContext("/mcp/initialize", new InitializeHandler());
		server.createContext("/mcp/tools/list", new ListToolsHandler());
		server.createContext("/mcp/tools/call", new CallToolHandler());
		server.createContext("/mcp/status", new StatusHandler());

		server.start();

		LOG.info("Server started on http://localhost:{}/mcp", port);
		LOG.info("Configure your MCP client with:");
		LOG.info("URL: http://localhost:{}/mcp", port);
		LOG.info("No authentication required");
	}

	void stop() {
		LOG.info("Stopping Server");
		server.stop(0);
		server = null;
	}

	private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
		addCorsHeaders(exchange);
		byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
		exchange.sendResponseHeaders(statusCode, responseBytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(responseBytes);
		}
	}

	private static void addCorsHeaders(HttpExchange exchange) {
		exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		exchange.getResponseHeaders().set("Access-Control-Allow-Headers", CONTENT_TYPE);
	}

	private static boolean handleCorsPreflightRequest(HttpExchange exchange) throws IOException {
		if ("OPTIONS".equals(exchange.getRequestMethod())) {
			addCorsHeaders(exchange);
			exchange.sendResponseHeaders(200, -1);
			return true;
		}
		return false;
	}


	private static String readRequestBody(HttpExchange exchange) throws IOException {
		return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}


	private class InitializeHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (handleCorsPreflightRequest(exchange)) {
				return;
			}

			if (!POST.equals(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(405, -1);
				return;
			}

			ObjectNode response = objectMapper.createObjectNode();
			response.put("protocolVersion", "1.0.0");

			ObjectNode serverInfo = response.putObject("serverInfo");
			serverInfo.put("name", serverName);
			serverInfo.put("version", serverVersion);

			ObjectNode capabilities = response.putObject("capabilities");
			capabilities.put("tools", true);
			capabilities.put("logging", true);

			sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
		}
	}

	private class ListToolsHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (handleCorsPreflightRequest(exchange)) {
				return;
			}

			if (!GET.equals(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(405, -1);
				return;
			}

			ArrayNode toolList = objectMapper.createArrayNode();
			for (HttpTool tool : tools.values()) {
				ObjectNode toolNode = toolList.addObject();
				toolNode.put("name", tool.name);
				toolNode.put("description", tool.description);
				// Parse the schema JSON string and add it as a node
				try {
					toolNode.set("inputSchema", objectMapper.readTree(tool.inputSchema));
				}
				catch (Exception e) {
					toolNode.put("inputSchema", tool.inputSchema); // Fallback to string
				}
			}

			ObjectNode response = objectMapper.createObjectNode();
			response.set("tools", toolList);

			sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
		}
	}

	private class CallToolHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (handleCorsPreflightRequest(exchange)) {
				return;
			}

			if (!POST.equals(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(405, -1);
				return;
			}

			try {
				String requestBody = readRequestBody(exchange);
				ObjectNode request = (ObjectNode) objectMapper.readTree(requestBody);

				String toolName = request.get("name").asText();
				Map<String, Object> arguments = new HashMap<>();
				if (request.has("arguments")) {
					arguments = objectMapper.convertValue(
									request.get("arguments"),
									objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
				}

				HttpTool tool = tools.get(toolName);
				if (tool == null) {
					ObjectNode error = objectMapper.createObjectNode();
					error.put("error", "Unknown tool: " + toolName);
					sendResponse(exchange, 404, objectMapper.writeValueAsString(error));
					return;
				}

				Object result = tool.handler.handle(arguments);

				ObjectNode response = objectMapper.createObjectNode();
				if (result instanceof String) {
					response.put("content", (String) result);
				}
				else {
					response.set("content", objectMapper.valueToTree(result));
				}
				response.put("isError", false);

				sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
			}
			catch (Exception e) {
				ObjectNode error = objectMapper.createObjectNode();
				error.put("error", "Tool execution failed: " + e.getMessage());
				error.put("isError", true);
				sendResponse(exchange, 500, objectMapper.writeValueAsString(error));
			}
		}
	}

	private class StatusHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (handleCorsPreflightRequest(exchange)) {
				return;
			}

			if (!GET.equals(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(405, -1);
				return;
			}

			ObjectNode status = objectMapper.createObjectNode();
			status.put("status", "running");
			status.put("serverName", serverName);
			status.put("serverVersion", serverVersion);
			status.put("toolCount", tools.size());
			status.put("authRequired", false);

			sendResponse(exchange, 200, objectMapper.writeValueAsString(status));
		}
	}
}