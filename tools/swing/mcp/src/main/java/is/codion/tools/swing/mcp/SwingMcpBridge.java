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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.swing.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * STDIO-to-HTTP bridge for MCP protocol.
 * Allows Claude Desktop to communicate with a running Codion application's HTTP MCP server.
 * <p>
 * This bridge reads JSON-RPC requests from stdin, forwards them to the HTTP server,
 * and writes responses back to stdout.
 * <p>
 * Usage:
 * <pre>
 * java -cp codion-tools-swing-mcp.jar is.codion.tools.swing.mcp.SwingMcpBridge [port]
 * </pre>
 * Default port is 8080.
 */
public final class SwingMcpBridge {

	private static final Logger LOG = LoggerFactory.getLogger(SwingMcpBridge.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String DEFAULT_PORT = "8080";
	private static final String MCP_BASE_PATH = "/mcp";
	private static final String JSONRPC = "jsonrpc";
	private static final String ERROR = "error";
	private static final String MESSAGE = "message";
	private static final String RESULT = "result";
	private static final String V2 = "2.0";
	private static final String ID = "id";

	private SwingMcpBridge() {}

	private static JsonNode handleRequest(String baseUrl, JsonNode request) throws Exception {
		String method = request.get("method").asText();
		JsonNode params = request.has("params") ? request.get("params") : MAPPER.createObjectNode();
		Object id = request.has(ID) ? request.get(ID) : null;

		switch (method) {
			case "initialize":
				return handleInitialize(baseUrl, params, id);
			case "tools/list":
				return handleToolsList(baseUrl, id);
			case "tools/call":
				return handleToolsCall(baseUrl, params, id);
			case "prompts/list":
				return handleEmptyList(id, "prompts");
			case "resources/list":
				return handleEmptyList(id, "resources");
			case "initialized":
			case "notifications/initialized":
				// Notifications, no response needed
				return null;
			default:
				return createErrorResponse(id, -32601, "Method not found: " + method);
		}
	}

	private static JsonNode handleInitialize(String baseUrl, JsonNode params, Object id) throws Exception {
		String url = baseUrl + "/initialize";
		JsonNode httpResponse = postRequest(url, params);
		ObjectNode response = MAPPER.createObjectNode();
		response.put(JSONRPC, V2);
		if (id != null) {
			response.set(ID, MAPPER.valueToTree(id));
		}
		response.set(RESULT, httpResponse);

		return response;
	}

	private static JsonNode handleToolsList(String baseUrl, Object id) throws Exception {
		String url = baseUrl + "/tools/list";
		JsonNode httpResponse = getRequest(url);
		ObjectNode response = MAPPER.createObjectNode();
		response.put(JSONRPC, V2);
		if (id != null) {
			response.set(ID, MAPPER.valueToTree(id));
		}
		response.set(RESULT, httpResponse);

		return response;
	}

	private static JsonNode handleToolsCall(String baseUrl, JsonNode params, Object id) throws Exception {
		String url = baseUrl + "/tools/call";
		JsonNode httpResponse = postRequest(url, params);
		ObjectNode response = MAPPER.createObjectNode();
		response.put(JSONRPC, V2);
		if (id != null) {
			response.set(ID, MAPPER.valueToTree(id));
		}
		if (httpResponse.has("isError") && httpResponse.get("isError").asBoolean()) {
			ObjectNode error = response.putObject(ERROR);
			error.put("code", -32000);
			error.put(MESSAGE, httpResponse.has("content") ? httpResponse.get("content").asText() : "Tool execution failed");
		}
		else {
			response.set(RESULT, httpResponse);
		}

		return response;
	}

	private static JsonNode getRequest(String url) throws Exception {
		HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
		try {
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			int responseCode = connection.getResponseCode();
			if (responseCode >= 200 && responseCode < 300) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF_8))) {
					return MAPPER.readTree(reader);
				}
			}
			else {
				String errorMessage = "HTTP " + responseCode;
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), UTF_8))) {
					errorMessage += ": " + reader.readLine();
				}
				throw new Exception(errorMessage);
			}
		}
		finally {
			connection.disconnect();
		}
	}

	private static JsonNode postRequest(String url, JsonNode body) throws Exception {
		HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
		try {
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), UTF_8)) {
				MAPPER.writeValue(writer, body);
			}
			int responseCode = connection.getResponseCode();
			if (responseCode >= 200 && responseCode < 300) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF_8))) {
					return MAPPER.readTree(reader);
				}
			}
			else {
				String errorMessage = "HTTP " + responseCode;
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), UTF_8))) {
					errorMessage += ": " + reader.readLine();
				}
				throw new Exception(errorMessage);
			}
		}
		finally {
			connection.disconnect();
		}
	}

	private static JsonNode handleEmptyList(Object id, String listName) {
		ObjectNode response = MAPPER.createObjectNode();
		response.put(JSONRPC, V2);
		if (id != null) {
			response.set(ID, MAPPER.valueToTree(id));
		}
		ObjectNode result = response.putObject(RESULT);
		result.putArray(listName);

		return response;
	}

	private static JsonNode createErrorResponse(Object id, int code, String message) {
		ObjectNode response = MAPPER.createObjectNode();
		response.put(JSONRPC, V2);
		if (id != null) {
			response.set(ID, MAPPER.valueToTree(id));
		}
		ObjectNode error = response.putObject(ERROR);
		error.put("code", code);
		error.put(MESSAGE, message);

		return response;
	}

	private static void runBridge(String baseUrl) throws IOException {
		try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, UTF_8));
				 PrintWriter stdout = new PrintWriter(new OutputStreamWriter(System.out, UTF_8), true)) {
			String line;
			while ((line = stdin.readLine()) != null) {
				handleRequest(baseUrl, line, stdout);
			}
		}
		catch (IOException e) {
			LOG.error("Bridge error: {}", e.getMessage(), e);
			throw e;
		}
	}

	private static void handleRequest(String baseUrl, String line, PrintWriter stdout) throws JsonProcessingException {
		JsonNode request = null;
		try {
			request = MAPPER.readTree(line);
			JsonNode response = handleRequest(baseUrl, request);
			if (response != null) {
				stdout.println(MAPPER.writeValueAsString(response));
				stdout.flush();
			}
		}
		catch (Exception e) {
			ObjectNode errorResponse = MAPPER.createObjectNode();
			errorResponse.put(JSONRPC, V2);
			if (request != null && request.has(ID)) {
				errorResponse.set(ID, request.get(ID));
			}
			ObjectNode error = errorResponse.putObject(ERROR);
			error.put("code", -32603);
			error.put(MESSAGE, "Internal error: " + e.getMessage());
			stdout.println(MAPPER.writeValueAsString(errorResponse));
			stdout.flush();
		}
	}

	public static void main(String[] args) throws IOException {
		String port = args.length > 0 ? args[0] : DEFAULT_PORT;
		String baseUrl = "http://localhost:" + port + MCP_BASE_PATH;
		runBridge(baseUrl);
	}
}
