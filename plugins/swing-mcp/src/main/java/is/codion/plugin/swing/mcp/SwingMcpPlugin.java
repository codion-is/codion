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
import is.codion.common.state.State;
import is.codion.common.version.Version;
import is.codion.plugin.swing.mcp.SwingMcpHttpServer.HttpTool;

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
import java.util.function.Consumer;

import static is.codion.common.Configuration.integerValue;
import static is.codion.plugin.swing.mcp.SwingMcpServer.*;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Plugin that integrates MCP server directly into a Codion application.
 * This allows AI tools to control the application via the Model Context Protocol over HTTP.
 * <p>
 * Configure with system properties:
 * -Dcodion.swing.mcp.http.port=8080     (sets HTTP port, default 8080)
 */
public final class SwingMcpPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(SwingMcpPlugin.class);


	/**
	 * System property to set the HTTP server port (default: 8080).
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: null
	 * </ul>
	 */
	public static final PropertyValue<Integer> HTTP_PORT = integerValue("codion.swing.mcp.http.port", 8080);

	// Plugin-specific constants
	private static final String WIDTH = "width";
	private static final String HEIGHT = "height";
	private static final String IMAGE = "image";
	private static final String BACKWARD = "backward";
	private static final String FORWARD = "forward";
	private static final String CODION_SWING_MCP = "codion-swing-mcp";
	private static final String SERVER_STARTUP_INFO = "Started MCP HTTP server for Swing application";
	private static final String SERVER_STOPPED_INFO = "Stopped MCP server";

	private final JComponent applicationComponent;

	private ExecutorService executor;
	private SwingMcpServer server;
	private SwingMcpHttpServer httpServer;

	private SwingMcpPlugin(JComponent applicationComponent) {
		this.applicationComponent = applicationComponent;
	}

	/**
	 * Create an MCP server for the given application component.
	 * @param applicationComponent the application component
	 * @return a {@link State} controlling the started state of this mcp server
	 */
	public static State mcpServer(JComponent applicationComponent) {
		SwingMcpPlugin plugin = new SwingMcpPlugin(requireNonNull(applicationComponent));

		return State.builder()
						.consumer(new ServerController(plugin))
						.build();
	}

	/**
	 * Start the MCP HTTP server in a daemon thread.
	 */
	private void start() {
		executor = newSingleThreadExecutor(new DaemonThreadFactory());
		executor.submit(this::runServer);
	}

	private void stop() {
		if (httpServer != null) {
			httpServer.stop();
			httpServer = null;
			LOG.info("Stopped MCP HTTP server");
		}
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
			LOG.info("Stopped MCP executor");
		}
		if (server != null) {
			server.stop();
			server = null;
		}
		System.out.println(SERVER_STOPPED_INFO);
	}

	private void runServer() {
		try {
			// Initialize the UI automation server
			server = new SwingMcpServer(applicationComponent);
			// Start HTTP server for MCP client access
			startHttpServer(server);
		}
		catch (AWTException e) {
			LOG.error("Failed to initialize Robot for UI automation: {}", e.getMessage());
		}
		catch (Exception e) {
			LOG.error("Failed to start MCP server: {}", e.getMessage());
		}
	}

	private void startHttpServer(SwingMcpServer swingMcpServer) throws IOException {
		httpServer = new SwingMcpHttpServer(HTTP_PORT.getOrThrow(), CODION_SWING_MCP, Version.versionString());

		// Register all UI automation tools with HTTP adapter
		registerHttpTools(httpServer, swingMcpServer);

		httpServer.start();
		LOG.info(SERVER_STARTUP_INFO);
		System.out.println(SERVER_STARTUP_INFO);
	}


	private static SwingMcpHttpServer.ToolHandler wrapWithErrorHandling(SwingMcpHttpServer.ToolHandler handler, String errorMessage) {
		return arguments -> {
			try {
				return handler.handle(arguments);
			}
			catch (Exception e) {
				throw new RuntimeException(errorMessage + ": " + e.getMessage(), e);
			}
		};
	}

	private static void registerHttpTools(SwingMcpHttpServer httpServer, SwingMcpServer swingMcpServer) {
		// Type text tool
		httpServer.addTool(new HttpTool(
						TYPE_TEXT, "Type text into the currently focused field",
						SwingMcpServer.createSchema(TEXT, STRING, "The text to type"),
						arguments -> {
							String text = (String) arguments.get(TEXT);
							swingMcpServer.typeText(text);

							return "Text typed successfully";
						}
		));

		// Key combination tool
		httpServer.addTool(new HttpTool(
						KEY_COMBO, "Press a key combination using Swing KeyStroke format",
						SwingMcpServer.createSchema("combo", STRING, "The key combination in Swing format (e.g., 'control alt UP', 'shift INSERT', 'alt A', 'control DOWN')"),
						arguments -> {
							String combo = (String) arguments.get("combo");
							swingMcpServer.keyCombo(combo);

							return "Key combination pressed";
						}
		));

		// Tab navigation tool
		httpServer.addTool(new HttpTool(
						TAB, "Press Tab to navigate fields",
						SwingMcpServer.createTwoPropertySchema(COUNT, NUMBER, "Number of times to press Tab (default: 1)",
										SHIFT, BOOLEAN, "Hold Shift for backward navigation (default: false)"),
						arguments -> {
							int count = integerParam(arguments, COUNT, 1);
							boolean shift = booleanParam(arguments, SHIFT, false);
							swingMcpServer.tab(count, shift);
							String direction = shift ? BACKWARD : FORWARD;

							return "Tabbed " + direction + " " + count + " times";
						}
		));


		// Application window screenshot tool
		httpServer.addTool(new HttpTool(
						APP_SCREENSHOT, "Take a screenshot of just the application window and return as base64",
						SwingMcpServer.createSchema(FORMAT, STRING, IMAGE_FORMAT + " (tip: use 'jpg' for better compression)"),
						arguments -> screenshotToBase64(arguments, swingMcpServer.takeApplicationScreenshot())
		));

		// Active window screenshot tool
		httpServer.addTool(new HttpTool(
						ACTIVE_WINDOW_SCREENSHOT, "Take a screenshot of the currently active window (dialog, popup, etc.) and return as base64",
						SwingMcpServer.createSchema(FORMAT, STRING, IMAGE_FORMAT + " (tip: use 'jpg' for better compression)"),
						arguments -> screenshotToBase64(arguments, swingMcpServer.takeActiveWindowScreenshot())
		));

		// Application window bounds tool
		httpServer.addTool(new HttpTool(
						APP_WINDOW_BOUNDS, "Get the application window bounds (x, y, width, height)",
						INPUT_SCHEMA,
						wrapWithErrorHandling(arguments -> {
							Rectangle bounds = swingMcpServer.getApplicationWindowBounds();

							return Map.of(
											"x", bounds.x,
											"y", bounds.y,
											WIDTH, bounds.width,
											HEIGHT, bounds.height);
						}, "Failed to get application window bounds")
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
						SwingMcpServer.createTwoPropertySchema("direction", STRING, "Direction: 'up', 'down', 'left', or 'right'",
										COUNT, NUMBER, "Number of times to press (default: 1)"),
						arguments -> {
							String direction = (String) arguments.get("direction");
							int count = integerParam(arguments, COUNT, 1);
							swingMcpServer.arrow(direction, count);

							return "Arrow " + direction + " pressed " + count + " times";
						}
		));
	}

	private static Map<String, Object> screenshotToBase64(Map<String, Object> arguments, BufferedImage screenshot) {
		return handleImageOperation(() -> {
			String format = (String) arguments.getOrDefault(FORMAT, PNG);
			if ("jpg".equalsIgnoreCase(format)) {
				format = "jpeg";
			}
			String base64 = SwingMcpServer.screenshotToBase64(screenshot, format);

			return Map.<String, Object>of(
							IMAGE, base64,
							WIDTH, screenshot.getWidth(),
							HEIGHT, screenshot.getHeight(),
							FORMAT, format);
		});
	}

	private static <T> T handleImageOperation(ImageOperation<T> operation) {
		try {
			return operation.execute();
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to encode screenshot: " + e.getMessage(), e);
		}
	}

	@FunctionalInterface
	private interface ImageOperation<T> {
		T execute() throws IOException;
	}


	private static final class ServerController implements Consumer<Boolean> {

		private final SwingMcpPlugin plugin;

		private ServerController(SwingMcpPlugin plugin) {
			this.plugin = plugin;
		}

		@Override
		public void accept(Boolean start) {
			if (start) {
				plugin.start();
			}
			else {
				plugin.stop();
			}
		}
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