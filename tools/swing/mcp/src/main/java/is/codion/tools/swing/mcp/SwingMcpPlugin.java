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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.swing.mcp;

import is.codion.common.reactive.state.State;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.version.Version;
import is.codion.tools.swing.mcp.SwingMcpHttpServer.HttpTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import static is.codion.common.utilities.Configuration.integerValue;
import static is.codion.tools.swing.mcp.SwingMcpServer.*;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Plugin that integrates MCP server directly into a Swing application.
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
	private static final String CODION_SWING_MCP = "codion-swing-mcp";
	private static final String SERVER_STARTUP_INFO = "Started MCP HTTP server for Swing application";
	private static final String SERVER_STOPPED_INFO = "Stopped MCP server";
	private static final String NARRATOR_NOT_AVAILABLE = "Narrator not available";
	private static final String KEY_SCHEMA = """
					{
						"type": "object",
						"properties": {
							"combo": {
								"type": "string",
								"description": "Key combination in AWT keystroke format. Examples: 'ENTER', 'shift ENTER', 'TAB', 'ctrl S', 'ctrl alt LEFT', 'shift TAB', 'alt F4', 'UP', 'DOWN', 'typed a', 'F5'"
							},
							"repeat": {
								"type": "integer",
								"description": "Number of times to repeat the keystroke (default: 1)"
							},
							"description": {
								"type": "string",
								"description": "Optional description of the action associated with this keystroke"
							}
						},
						"required": ["combo"]
					}
					""";

	private final JComponent applicationComponent;
	private final boolean includeNarrator;

	private ExecutorService executor;
	private SwingMcpServer server;
	private SwingMcpHttpServer httpServer;

	private SwingMcpPlugin(JComponent applicationComponent, boolean includeNarrator) {
		this.applicationComponent = applicationComponent;
		this.includeNarrator = includeNarrator;
	}

	/**
	 * Create an MCP server for the given application component.
	 * @param applicationComponent the application component
	 * @param includeNarrator if true a {@link is.codion.tools.swing.robot.Narrator} is included
	 * @return a {@link State} controlling the started state of this mcp server
	 */
	public static State mcpServer(JComponent applicationComponent, boolean includeNarrator) {
		SwingMcpPlugin plugin = new SwingMcpPlugin(requireNonNull(applicationComponent), includeNarrator);

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
		LOG.info(SERVER_STOPPED_INFO);
	}

	private void runServer() {
		try {
			// Initialize the UI automation server
			server = new SwingMcpServer(applicationComponent, includeNarrator);
			// Start HTTP server for MCP client access
			startHttpServer(server);
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
							swingMcpServer.type(text);

							return "Text typed successfully";
						}
		));

		// Key combination tool - handles all keyboard input
		httpServer.addTool(new HttpTool(
						KEY_COMBO, "Press a key combination using AWT KeyStroke format",
						KEY_SCHEMA,
						arguments -> {
							String combo = (String) arguments.get("combo");
							int repeat = SwingMcpServer.integerParam(arguments, "repeat", 1);
							String description = (String) arguments.get("description");
							swingMcpServer.key(combo, repeat, description);

							String message = repeat > 1
											? String.format("Key combination '%s' pressed %d times", combo, repeat)
											: String.format("Key combination '%s' pressed", combo);
							if (description != null) {
								message += " (" + description + ")";
							}
							return message;
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

		// Clear field tool
		httpServer.addTool(new HttpTool(
						CLEAR_FIELD, "Clear the current field by selecting all and deleting",
						INPUT_SCHEMA,
						arguments -> {
							swingMcpServer.clearField();

							return "Field cleared";
						}
		));

		// Narrator tools (only added if narrator is available)
		if (swingMcpServer.narratorAvailable()) {
			// Narrate tool
			httpServer.addTool(new HttpTool(
							"narrate", "Add narration text to the narrator window",
							SwingMcpServer.createSchema(TEXT, STRING, "The narration text to display"),
							arguments -> {
								String text = (String) arguments.get(TEXT);
								if (swingMcpServer.narrate(text)) {
									return "Narration added successfully";
								}

								return NARRATOR_NOT_AVAILABLE;
							}
			));

			// Clear narration tool
			httpServer.addTool(new HttpTool(
							"clear_narration", "Clear all narration text from the narrator window",
							INPUT_SCHEMA,
							arguments -> {
								if (swingMcpServer.clearNarration()) {
									return "Narration cleared successfully";
								}

								return NARRATOR_NOT_AVAILABLE;
							}
			));

			// Clear keystrokes tool
			httpServer.addTool(new HttpTool(
							"clear_keystrokes", "Clear the keystroke history from the narrator window",
							INPUT_SCHEMA,
							arguments -> {
								if (swingMcpServer.clearKeyStrokes()) {
									return "Keystrokes cleared successfully";
								}

								return NARRATOR_NOT_AVAILABLE;
							}
			));
		}
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
			throw new UncheckedIOException("Failed to encode screenshot: " + e.getMessage(), e);
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