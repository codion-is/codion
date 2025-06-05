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

import is.codion.common.property.PropertyValue;
import is.codion.common.version.Version;
import is.codion.plugin.swing.mcp.SwingMcpHttpServer.HttpTool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.integerValue;
import static is.codion.plugin.swing.mcp.SwingMcpServer.*;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Plugin that integrates MCP server directly into a Codion application.
 * This allows AI tools to control the application via the Model Context Protocol.
 * <p>
 * The plugin supports two modes:
 * 1. STDIO mode - For subprocess-based clients like Claude Desktop
 * 2. HTTP mode - For any client that can make HTTP requests
 * <p>
 * Configure with system properties:
 * -Dcodion.swing.mcp.http.enabled=true  (enables HTTP mode)
 * -Dcodion.swing.mcp.http.port=8080     (sets HTTP port, default 8080)
 */
public final class SwingMcpPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(SwingMcpPlugin.class);

	/**
	 * System property to enable HTTP mode instead of STDIO.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> HTTP_ENABLED = booleanValue("codion.swing.mcp.http.enabled", true);

	/**
	 * System property to set the HTTP server port (default: 8080).
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: null
	 * </ul>
	 */
	public static final PropertyValue<Integer> HTTP_PORT = integerValue("codion.swing.mcp.http.port", 8080);

	private static final String STRING = "string";
	private static final String TEXT = "text";
	private static final String COUNT = "count";
	private static final String FORMAT = "format";
	private static final String WIDTH = "width";
	private static final String HEIGHT = "height";
	private static final String IMAGE = "image";
	private static final String INPUT_SCHEMA = "{\"type\": \"object\", \"properties\": {}}";
	private static final String FOCUS_WINDOW = "focus_window";
	private static final String ENTER = "enter";
	private static final String ESCAPE = "escape";
	private static final String CLEAR_FIELD = "clear_field";
	private static final String ARROW = "arrow";
	private static final String APP_SCREENSHOT = "app_screenshot";
	private static final String PNG = "png";
	private static final String IMAGE_FORMAT = "Image format: 'png' or 'jpg' (default: 'png')";
	private static final String APP_WINDOW_BOUNDS = "app_window_bounds";
	private static final String SCREENSHOT = "screenshot";
	private static final String KEY_COMBO = "key_combo";
	private static final String TAB = "tab";
	private static final String TYPE_TEXT = "type_text";
	private static final String SHIFT = "shift";
	private static final String BOOLEAN = "boolean";
	private static final String NUMBER = "number";
	private static final String BACKWARD = "backward";
	private static final String FORWARD = "forward";
	private static final String CODION_SWING_MCP = "codion-swing-mcp";
	private static final String MCP_SERVER_NAME = "MCP_SERVER_NAME";
	private static final String MCP_STDIO = "mcp.stdio";

	private final JComponent applicationComponent;
	private final ExecutorService executor = newSingleThreadExecutor(new DaemonThreadFactory());

	private SwingMcpPlugin(JComponent applicationComponent) {
		this.applicationComponent = applicationComponent;
	}

	/**
	 * Start the MCP server for the given application component.
	 * @param applicationComponent the application component
	 */
	public static void startMcpServer(JComponent applicationComponent) {
		new SwingMcpPlugin(requireNonNull(applicationComponent)).start();
	}

	/**
	 * Start the MCP server in a daemon thread.
	 * The mode (STDIO or HTTP) is determined by system properties and environment.
	 */
	private void start() {
		executor.submit(this::runServer);
	}

	private void runServer() {
		try {
			// Initialize the UI automation server
			SwingMcpServer server = new SwingMcpServer(applicationComponent);
			if (HTTP_ENABLED.getOrThrow()) {
				// HTTP mode - accessible from any MCP client
				startHttpServer(server);
			}
			else if (System.console() != null || isStdioAvailable()) {
				// STDIO mode - for subprocess-based clients
				startStdioServer(server);
			}
			else {
				LOG.warn("MCP server not started. No STDIO available and HTTP mode not enabled.");
				LOG.warn("To enable HTTP mode, use: -D{}=true", HTTP_ENABLED);
			}
		}
		catch (AWTException e) {
			LOG.error("Failed to initialize Robot for UI automation: {}", e.getMessage());
		}
		catch (Exception e) {
			LOG.error("Failed to start MCP server: {}", e.getMessage());
		}
	}

	private static void startHttpServer(SwingMcpServer swingMcpServer) throws IOException {
		SwingMcpHttpServer httpServer = new SwingMcpHttpServer(HTTP_PORT.getOrThrow(), CODION_SWING_MCP, Version.versionString());

		// Register all UI automation tools with HTTP adapter
		registerHttpTools(httpServer, swingMcpServer);

		httpServer.start();
		LOG.info("Started MCP HTTP server for Swing application");
	}

	private static void startStdioServer(SwingMcpServer swingMcpServer) {
		try {
			// Create STDIO transport provider
			StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(new ObjectMapper());
			// Create MCP server with capabilities
			McpSyncServer mcpServer = McpServer.sync(transportProvider)
							.serverInfo(CODION_SWING_MCP, Version.versionString())
							.capabilities(ServerCapabilities.builder()
											.tools(true) // Enable tool support
											.logging() // Enable logging support
											.build())
							.build();

			// Register UI automation tools
			swingMcpServer.registerTools(mcpServer);

			LOG.info("Swing MCP STDIO Server ready for AI connections");
			LOG.info("Available tools: UI automation (keyboard, screenshots, mouse)");

			// The server runs indefinitely on this daemon thread
			// It will be terminated when the JVM shuts down
			Thread.currentThread().join();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.debug("MCP server thread interrupted");
		}
		catch (RuntimeException e) {
			// This is expected when running without an MCP client connected
			if (e.getMessage() != null && e.getMessage().contains("Failed to enqueue message")) {
				LOG.info("No MCP client connected. The server is ready but waiting for a client.");
				LOG.info("To use MCP, connect with Claude Desktop, Continue.dev, or another MCP client.");
			}
			else {
				LOG.error("MCP Server runtime error: {}", e.getMessage());
			}
		}
		catch (Exception e) {
			LOG.error("MCP Server error: {}", e.getMessage());
		}
	}

	private static boolean isStdioAvailable() {
		// Check if we're running in an environment where STDIO might be piped
		// This is a heuristic - MCP clients typically set certain environment variables
		return System.getenv(MCP_SERVER_NAME) != null || System.getProperty(MCP_STDIO) != null;
	}

	private static void registerHttpTools(SwingMcpHttpServer httpServer, SwingMcpServer swingMcpServer) {
		// Type text tool
		httpServer.addTool(new HttpTool(
						TYPE_TEXT, "Type text into the currently focused field",
						createSchema(TEXT, STRING, "The text to type"),
						arguments -> {
							String text = (String) arguments.get(TEXT);
							swingMcpServer.typeText(text);

							return "Text typed successfully";
						}
		));

		// Key combination tool
		httpServer.addTool(new HttpTool(
						KEY_COMBO, "Press a key combination using Swing KeyStroke format",
						createSchema("combo", STRING, "The key combination in Swing format (e.g., 'control alt UP', 'shift INSERT', 'alt A', 'control DOWN')"),
						arguments -> {
							String combo = (String) arguments.get("combo");
							swingMcpServer.keyCombo(combo);

							return "Key combination pressed";
						}
		));

		// Tab navigation tool
		httpServer.addTool(new HttpTool(
						TAB, "Press Tab to navigate fields",
						createComplexSchema(COUNT, NUMBER, "Number of times to press Tab (default: 1)",
										SHIFT, BOOLEAN, "Hold Shift for backward navigation (default: false)"),
						arguments -> {
							int count = integerParam(arguments, COUNT, 1);
							boolean shift = booleanParam(arguments, SHIFT, false);
							swingMcpServer.tab(count, shift);
							String direction = shift ? BACKWARD : FORWARD;

							return "Tabbed " + direction + " " + count + " times";
						}
		));

		// Desktop screenshot tool
		httpServer.addTool(new HttpTool(
						SCREENSHOT, "Take a screenshot of the entire desktop and return as base64",
						createSchema(FORMAT, STRING, IMAGE_FORMAT),
						arguments -> {
							try {
								String format = (String) arguments.getOrDefault(FORMAT, PNG);
								BufferedImage screenshot = swingMcpServer.takeScreenshot();
								String base64 = screenshotToBase64(screenshot, format);

								// Return as a structured object
								return Map.<String, Object>of(
												IMAGE, base64,
												WIDTH, screenshot.getWidth(),
												HEIGHT, screenshot.getHeight(),
												FORMAT, format);
							}
							catch (IOException e) {
								throw new RuntimeException("Failed to take screenshot: " + e.getMessage(), e);
							}
						}
		));

		// Application window screenshot tool
		httpServer.addTool(new HttpTool(
						APP_SCREENSHOT, "Take a screenshot of just the application window and return as base64",
						createSchema(FORMAT, STRING, IMAGE_FORMAT),
						arguments -> {
							try {
								String format = (String) arguments.getOrDefault(FORMAT, PNG);
								BufferedImage screenshot = swingMcpServer.takeApplicationScreenshot();
								String base64 = screenshotToBase64(screenshot, format);

								// Return as a structured object
								return Map.<String, Object>of(
												IMAGE, base64,
												WIDTH, screenshot.getWidth(),
												HEIGHT, screenshot.getHeight(),
												FORMAT, format);
							}
							catch (IOException e) {
								throw new RuntimeException("Failed to take application screenshot: " + e.getMessage(), e);
							}
						}
		));

		// Application window bounds tool
		httpServer.addTool(new HttpTool(
						APP_WINDOW_BOUNDS, "Get the application window bounds (x, y, width, height)",
						INPUT_SCHEMA,
						arguments -> {
							try {
								Rectangle bounds = swingMcpServer.getApplicationWindowBounds();

								return Map.of(
												"x", bounds.x,
												"y", bounds.y,
												WIDTH, bounds.width,
												HEIGHT, bounds.height);
							}
							catch (Exception e) {
								throw new RuntimeException("Failed to get application window bounds: " + e.getMessage(), e);
							}
						}
		));

		// Focus application window tool
		httpServer.addTool(new HttpTool(
						FOCUS_WINDOW, "Bring the application window to front and focus it",
						INPUT_SCHEMA,
						arguments -> {
							swingMcpServer.focusWindow();

							return "Application window focused";
						}
		));

		// Enter key tool
		httpServer.addTool(new HttpTool(
						ENTER, "Press Enter key (transfers focus between fields in Codion apps)",
						INPUT_SCHEMA,
						arguments -> {
							swingMcpServer.enter();

							return "Enter key pressed";
						}
		));

		// Escape key tool  
		httpServer.addTool(new HttpTool(
						ESCAPE, "Press Escape key",
						INPUT_SCHEMA,
						arguments -> {
							swingMcpServer.escape();

							return "Escape key pressed";
						}
		));

		// Clear field tool
		httpServer.addTool(new HttpTool(
						CLEAR_FIELD, "Clear the current field by selecting all and deleting",
						INPUT_SCHEMA,
						arguments -> {
							swingMcpServer.clearField();
							return "Field cleared";
						}
		));

		// Arrow key navigation tool
		httpServer.addTool(new HttpTool(
						ARROW, "Press arrow keys for navigation",
						createComplexSchema("direction", STRING, "Direction: 'up', 'down', 'left', or 'right'",
										COUNT, NUMBER, "Number of times to press (default: 1)"),
						arguments -> {
							String direction = (String) arguments.get("direction");
							int count = integerParam(arguments, COUNT, 1);
							swingMcpServer.arrow(direction, count);

							return "Arrow " + direction + " pressed " + count + " times";
						}
		));
	}

	private static String createSchema(String propName, String propType, String propDesc) {
		return String.format("""
						{
							"type": "object",
							"properties": {
								"%s": {
									"type": "%s",
									"description": "%s"
								}
							},
							"required": ["%s"]
						}
						""", propName, propType, propDesc, propName);
	}

	private static String createComplexSchema(String prop1Name, String prop1Type, String prop1Desc,
																						String prop2Name, String prop2Type, String prop2Desc) {
		return String.format("""
						{
							"type": "object",
							"properties": {
								"%s": {
									"type": "%s",
									"description": "%s"
								},
								"%s": {
									"type": "%s",
									"description": "%s"
								}
							}
						}
						""", prop1Name, prop1Type, prop1Desc, prop2Name, prop2Type, prop2Desc);
	}

	private static final class DaemonThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);

			return thread;
		}
	}
}