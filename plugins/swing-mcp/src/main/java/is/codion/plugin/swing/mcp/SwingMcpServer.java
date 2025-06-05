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

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * MCP Server for UI automation of Swing applications.
 * Provides keyboard control, screenshot capabilities, and MCP protocol integration.
 * Can be used both as a standalone MCP server or embedded in applications.
 */
final class SwingMcpServer {

	private static final Logger LOG = LoggerFactory.getLogger(SwingMcpServer.class);

	private static final String KEYBOARD = "keyboard";
	private static final String NUMBER = "number";
	private static final String TYPE = "type";
	private static final String COUNT = "count";
	private static final String SHIFT = "shift";
	private static final String STRING = "string";
	private static final String FORMAT = "format";
	private static final String SCREENSHOT = "screenshot";
	private static final String WAIT = "wait";
	private static final String CLEAR_FIELD = "clear_field";
	private static final String ESCAPE = "escape";
	private static final String ENTER = "enter";
	private static final String ARROW = "arrow";
	private static final String DESCRIPTION = "description";
	private static final String DIRECTION = "direction";
	private static final String TYPE_OBJECT = "{\"type\": \"object\"}";
	private static final String APP_SCREENSHOT = "app_screenshot";
	private static final String IMAGE_DIMENSIONS = """
					{
						"image": "%s",
						"width": %d,
						"height": %d,
						"format": "%s"
					}""";
	private static final String APP_WINDOW_BOUNDS = "app_window_bounds";
	private static final String WINDOW_BOUNDS = """
					{
						"x": %d,
						"y": %d,
						"width": %d,
						"height": %d
					}""";
	private static final String SAVE_SCREENSHOT = "save_screenshot";
	private static final String FILENAME = "filename";
	private static final String APP_ONLY = "app_only";
	private static final String BOOLEAN = "boolean";
	private static final String PNG = "png";
	private static final String IMAGE_FORMAT = "Image format: 'png' or 'jpg' (default: 'png')";
	private static final String SCREENSHOT_DATA = """
					{
						"saved": true,
						"path": "%s",
						"app_only": %s
					}""";
	private static final String SCREEN_SIZE = "screen_size";
	private static final String SCREEN_SIZE_DATA = """
					{
						"width": %d,
						"height": %d
					}""";
	private static final String FOCUS_WINDOW = "focus_window";
	private static final String CLICK_AT = "click_at";
	private static final String X = "x";
	private static final String Y = "y";
	private static final String MOUSE = "mouse";
	private static final String KEY_COMBO = "key_combo";
	private static final String COMBO = "combo";
	private static final String TAB = "tab";

	private final Robot robot;
	private final KeyboardController keyboardController;
	private final JComponent applicationComponent;

	SwingMcpServer(JComponent applicationComponent) throws AWTException {
		this.applicationComponent = requireNonNull(applicationComponent);
		this.robot = new Robot();
		this.keyboardController = new KeyboardController(robot);
		// Configure robot for smooth operation
		robot.setAutoDelay(50); // Small delay between events for reliability
		robot.setAutoWaitForIdle(true); // Wait for events to be processed
	}

	/**
	 * Register all UI automation tools with an MCP server.
	 * @param mcpServer the MCP server to register tools with
	 */
	void registerTools(McpSyncServer mcpServer) {
		registerKeyboardTools(mcpServer, this);
		registerScreenshotTools(mcpServer, this);
		registerWindowTools(mcpServer, this);
	}

	/**
	 * Type text into the currently focused field
	 * @param text the text to type
	 */
	void typeText(String text) {
		LOG.debug("Typing text: {}", text);
		keyboardController.typeText(text);
	}

	/**
	 * Press a key combination like Alt+A or Ctrl+S
	 * @param combo the key combination (e.g., "Alt+A", "Ctrl+Shift+S")
	 */
	void keyCombo(String combo) {
		LOG.debug("Key combo: {}", combo);
		keyboardController.pressKeyCombo(combo);
	}

	/**
	 * Press Tab to navigate fields
	 * @param count number of times to press Tab
	 * @param shift whether to hold Shift (for backward navigation)
	 */
	void tab(int count, boolean shift) {
		keyboardController.tab(count, shift);
	}

	/**
	 * Press Enter key
	 */
	void enter() {
		keyboardController.enter();
	}

	/**
	 * Press Escape key
	 */
	void escape() {
		keyboardController.escape();
	}

	/**
	 * Press arrow keys for navigation
	 * @param direction "up", "down", "left", or "right"
	 * @param count number of times to press the arrow key
	 */
	void arrow(String direction, int count) {
		keyboardController.arrow(direction, count);
	}

	/**
	 * Clear the current field by selecting all and deleting
	 */
	void clearField() {
		keyboardController.clearField();
	}

	/**
	 * Take a screenshot of the entire screen
	 * @return the screenshot as a BufferedImage
	 */
	BufferedImage takeScreenshot() {
		Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

		return robot.createScreenCapture(screenRect);
	}

	/**
	 * Get the bounds of the application window
	 * @return Rectangle with x, y, width, height of the application window
	 */
	Rectangle getApplicationWindowBounds() {
		Window window = getApplicationWindow();

		return window.getBounds();
	}

	/**
	 * Take a screenshot of just the application window
	 * @return the screenshot as a BufferedImage
	 */
	BufferedImage takeApplicationScreenshot() {
		Rectangle bounds = getApplicationWindowBounds();

		return robot.createScreenCapture(bounds);
	}

	/**
	 * Bring the application window to front (public method for MCP tools)
	 */
	void focusWindow() {
		focusApplicationWindow();
	}

	/**
	 * Click at specific coordinates to focus a window
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	void clickAt(int x, int y) {
		robot.mouseMove(x, y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private Window getApplicationWindow() {
		Window window = (Window) SwingUtilities.getAncestorOfClass(Window.class, applicationComponent);
		if (window == null) {
			throw new IllegalStateException("No application window found");
		}

		return window;
	}

	/**
	 * Bring the application window to front and ensure it has focus
	 */
	private void focusApplicationWindow() {
		try {
			getApplicationWindow().toFront();
		}
		catch (Exception e) {
			LOG.warn("Could not focus application window", e);
			// Continue anyway - the action might still work
		}
	}

	/**
	 * Wait for specified milliseconds
	 * @param ms milliseconds to wait
	 */
	static void wait(int ms) {
		try {
			TimeUnit.MILLISECONDS.sleep(ms);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Save a screenshot to a file
	 * @param image the image to save
	 * @param file the file to save to
	 * @param format the image format (e.g., "png", "jpg")
	 * @throws IOException if saving fails
	 */
	static void saveScreenshot(BufferedImage image, File file, String format) throws IOException {
		ImageIO.write(image, format, file);
	}

	/**
	 * Convert a screenshot to Base64 for transmission over MCP
	 * @param image the image to convert
	 * @param format the image format (e.g., "png", "jpg")
	 * @return Base64 encoded string
	 * @throws IOException if conversion fails
	 */
	static String screenshotToBase64(BufferedImage image, String format) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, format, baos);

			return Base64.getEncoder().encodeToString(baos.toByteArray());
		}
	}

	/**
	 * Get the current screen size
	 * @return the screen dimensions
	 */
	static Dimension getScreenSize() {
		return Toolkit.getDefaultToolkit().getScreenSize();
	}

	static int integerParam(Map<String, ?> args, String key, int defaultValue) {
		Object value = args.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}

		return defaultValue;
	}

	static boolean booleanParam(Map<String, ?> args, String key, boolean defaultValue) {
		Object value = args.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		}

		return defaultValue;
	}

	/**
	 * Keyboard controller with high-level operations
	 */
	private static class KeyboardController {

		private final Robot robot;

		private KeyboardController(Robot robot) {
			this.robot = robot;
		}

		void typeText(String text) {
			for (char c : text.toCharArray()) {
				typeChar(c);
			}
		}

		private void pressKeyCombo(String combo) {
			try {
				// Use combo directly as Swing KeyStroke format
				LOG.debug("Using key combo: '{}'", combo);

				KeyStroke keyStroke = KeyStroke.getKeyStroke(combo);
				if (keyStroke == null) {
					throw new IllegalArgumentException("Invalid key combination: " + combo);
				}

				int keyCode = keyStroke.getKeyCode();
				int modifiers = keyStroke.getModifiers();

				LOG.debug("KeyStroke - keyCode: {} (char: {}), modifiers: {} (binary: {})",
								keyCode, KeyEvent.getKeyText(keyCode), modifiers, Integer.toBinaryString(modifiers));

				// Press modifier keys first
				if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
					LOG.debug("Pressing CTRL");
					robot.keyPress(KeyEvent.VK_CONTROL);
				}
				if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
					LOG.debug("Pressing ALT");
					robot.keyPress(KeyEvent.VK_ALT);
				}
				if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
					LOG.debug("Pressing SHIFT");
					robot.keyPress(KeyEvent.VK_SHIFT);
				}
				if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
					LOG.debug("Pressing META");
					robot.keyPress(KeyEvent.VK_META);
				}

				// Press the main key (but don't release yet)
				LOG.debug("Pressing main key: {}", KeyEvent.getKeyText(keyCode));
				robot.keyPress(keyCode);

				// Small delay to ensure key registration
				delay();

				// Release the main key first
				robot.keyRelease(keyCode);
				LOG.debug("Released main key: {}", KeyEvent.getKeyText(keyCode));

				// Release modifier keys in reverse order
				if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
					robot.keyRelease(KeyEvent.VK_META);
				}
				if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
					robot.keyRelease(KeyEvent.VK_SHIFT);
				}
				if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
					robot.keyRelease(KeyEvent.VK_ALT);
				}
				if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
					robot.keyRelease(KeyEvent.VK_CONTROL);
				}
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Failed to parse key combination '" + combo + "': " + e.getMessage(), e);
			}
		}

		private static void delay() {
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		private void tab(int count, boolean shift) {
			for (int i = 0; i < count; i++) {
				if (shift) {
					robot.keyPress(KeyEvent.VK_SHIFT);
				}
				robot.keyPress(KeyEvent.VK_TAB);
				robot.keyRelease(KeyEvent.VK_TAB);
				if (shift) {
					robot.keyRelease(KeyEvent.VK_SHIFT);
				}
			}
		}

		private void enter() {
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.keyRelease(KeyEvent.VK_ENTER);
		}

		private void escape() {
			robot.keyPress(KeyEvent.VK_ESCAPE);
			robot.keyRelease(KeyEvent.VK_ESCAPE);
		}

		private void arrow(String direction, int count) {
			int keyCode = switch (direction.toLowerCase()) {
				case "up" -> KeyEvent.VK_UP;
				case "down" -> KeyEvent.VK_DOWN;
				case "left" -> KeyEvent.VK_LEFT;
				case "right" -> KeyEvent.VK_RIGHT;
				default -> throw new IllegalArgumentException("Invalid direction: " + direction);
			};

			for (int i = 0; i < count; i++) {
				robot.keyPress(keyCode);
				robot.keyRelease(keyCode);
			}
		}

		private void clearField() {
			// Select all
			robot.keyPress(KeyEvent.VK_CONTROL);
			robot.keyPress(KeyEvent.VK_A);
			robot.keyRelease(KeyEvent.VK_A);
			robot.keyRelease(KeyEvent.VK_CONTROL);

			// Delete
			robot.keyPress(KeyEvent.VK_DELETE);
			robot.keyRelease(KeyEvent.VK_DELETE);
		}

		private void typeChar(char c) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
			if (KeyEvent.CHAR_UNDEFINED == keyCode) {
				throw new RuntimeException("Cannot type character: " + c);
			}

			boolean needShift = Character.isUpperCase(c) || "!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;

			if (needShift) {
				robot.keyPress(KeyEvent.VK_SHIFT);
			}

			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);

			if (needShift) {
				robot.keyRelease(KeyEvent.VK_SHIFT);
			}
		}
	}

	private static void registerKeyboardTools(McpSyncServer server, SwingMcpServer swingMcpServer) {
		// Type text tool
		server.addTool(new SyncToolSpecification(
						new Tool("type_text", "Type text into the currently focused field",
										createSchema("text", STRING, "The text to type")),
						(exchange, arguments) -> {
							String text = (String) arguments.get("text");
							swingMcpServer.typeText(text);

							exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
											.level(McpSchema.LoggingLevel.INFO)
											.logger(KEYBOARD)
											.data("Typed: " + text)
											.build());

							return new CallToolResult("Text typed successfully", false);
						}
		));

		// Key combination tool
		server.addTool(new SyncToolSpecification(
						new Tool(KEY_COMBO, "Press a key combination like Alt+A or Ctrl+S",
										createSchema(COMBO, STRING, "The key combination (e.g., 'Alt+A', 'Ctrl+Shift+S')")),
						(exchange, arguments) -> {
							String combo = (String) arguments.get(COMBO);
							swingMcpServer.keyCombo(combo);

							exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
											.level(McpSchema.LoggingLevel.INFO)
											.logger(KEYBOARD)
											.data("Pressed: " + combo)
											.build());

							return new CallToolResult("Key combination pressed", false);
						}
		));

		// Tab navigation tool
		server.addTool(new SyncToolSpecification(
						new Tool(TAB, "Press Tab to navigate fields",
										createSchema(Map.of(
														COUNT, Map.of(TYPE, NUMBER, DESCRIPTION, "Number of times to press Tab (default: 1)"),
														SHIFT, Map.of(TYPE, BOOLEAN, DESCRIPTION, "Hold Shift for backward navigation (default: false)")
										))),
						(exchange, arguments) -> {
							int count = integerParam(arguments, COUNT, 1);
							boolean shift = booleanParam(arguments, SHIFT, false);
							swingMcpServer.tab(count, shift);

							String direction = shift ? "backward" : "forward";
							exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
											.level(McpSchema.LoggingLevel.INFO)
											.logger(KEYBOARD)
											.data("Tabbed " + direction + " " + count + " times")
											.build());

							return new CallToolResult("Tab navigation complete", false);
						}
		));

		// Arrow key navigation tool
		server.addTool(new SyncToolSpecification(
						new Tool(ARROW, "Press arrow keys for navigation",
										createSchema(Map.of(
														DIRECTION, Map.of(TYPE, STRING, DESCRIPTION, "Direction: 'up', 'down', 'left', or 'right'"),
														COUNT, Map.of(TYPE, NUMBER, DESCRIPTION, "Number of times to press (default: 1)")
										))),
						(exchange, arguments) -> {
							String direction = (String) arguments.get(DIRECTION);
							int count = integerParam(arguments, COUNT, 1);
							swingMcpServer.arrow(direction, count);

							exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
											.level(McpSchema.LoggingLevel.INFO)
											.logger(KEYBOARD)
											.data("Arrow " + direction + " pressed " + count + " times")
											.build());

							return new CallToolResult("Arrow navigation complete", false);
						}
		));

		// Enter key tool
		server.addTool(new SyncToolSpecification(
						new Tool(ENTER, "Press Enter key", TYPE_OBJECT),
						(exchange, arguments) -> {
							swingMcpServer.enter();
							return new CallToolResult("Enter pressed", false);
						}
		));

		// Escape key tool
		server.addTool(new SyncToolSpecification(
						new Tool(ESCAPE, "Press Escape key", TYPE_OBJECT),
						(exchange, arguments) -> {
							swingMcpServer.escape();
							return new CallToolResult("Escape pressed", false);
						}
		));

		// Clear field tool
		server.addTool(new SyncToolSpecification(
						new Tool(CLEAR_FIELD, "Clear the current field by selecting all and deleting", TYPE_OBJECT),
						(exchange, arguments) -> {
							swingMcpServer.clearField();
							return new CallToolResult("Field cleared", false);
						}
		));

		// Wait tool
		server.addTool(new SyncToolSpecification(
						new Tool(WAIT, "Wait for specified milliseconds",
										createSchema("ms", NUMBER, "Milliseconds to wait")),
						(exchange, arguments) -> {
							int ms = integerParam(arguments, "ms", 1000);
							SwingMcpServer.wait(ms);
							return new CallToolResult("Waited " + ms + "ms", false);
						}
		));
	}

	private static void registerScreenshotTools(McpSyncServer server, SwingMcpServer swingMcpServer) {
		// Take desktop screenshot tool
		server.addTool(new SyncToolSpecification(
						new Tool(SCREENSHOT, "Take a screenshot of the entire desktop and return as base64",
										createSchema(FORMAT, STRING, IMAGE_FORMAT)),
						(exchange, arguments) -> {
							try {
								String format = (String) arguments.getOrDefault(FORMAT, PNG);
								BufferedImage screenshot = swingMcpServer.takeScreenshot();
								String base64 = SwingMcpServer.screenshotToBase64(screenshot, format);

								exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
												.level(McpSchema.LoggingLevel.INFO)
												.logger(SCREENSHOT)
												.data("Desktop screenshot taken (" + format + ")")
												.build());

								// Return screenshot data as JSON text
								String result = String.format(IMAGE_DIMENSIONS, base64, screenshot.getWidth(), screenshot.getHeight(), format);

								return new CallToolResult(result, false);
							}
							catch (IOException e) {
								return new CallToolResult("Failed to take screenshot: " + e.getMessage(), true);
							}
						}
		));

		// Take application window screenshot tool
		server.addTool(new SyncToolSpecification(
						new Tool(APP_SCREENSHOT, "Take a screenshot of just the application window and return as base64",
										createSchema(FORMAT, STRING, IMAGE_FORMAT)),
						(exchange, arguments) -> {
							try {
								String format = (String) arguments.getOrDefault(FORMAT, PNG);
								BufferedImage screenshot = swingMcpServer.takeApplicationScreenshot();
								String base64 = SwingMcpServer.screenshotToBase64(screenshot, format);

								exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
												.level(McpSchema.LoggingLevel.INFO)
												.logger(SCREENSHOT)
												.data("Application screenshot taken (" + format + ")")
												.build());

								// Return screenshot data as JSON text
								String result = String.format(IMAGE_DIMENSIONS, base64, screenshot.getWidth(), screenshot.getHeight(), format);

								return new CallToolResult(result, false);
							}
							catch (IOException e) {
								return new CallToolResult("Failed to take application screenshot: " + e.getMessage(), true);
							}
						}
		));

		// Get application window bounds tool
		server.addTool(new SyncToolSpecification(
						new Tool(APP_WINDOW_BOUNDS, "Get the application window bounds (x, y, width, height)", TYPE_OBJECT),
						(exchange, arguments) -> {
							try {
								Rectangle bounds = swingMcpServer.getApplicationWindowBounds();

								return new CallToolResult(String.format(WINDOW_BOUNDS, bounds.x, bounds.y, bounds.width, bounds.height), false);
							}
							catch (Exception e) {
								return new CallToolResult("Failed to get application window bounds: " + e.getMessage(), true);
							}
						}
		));

		// Save screenshot tool
		server.addTool(new SyncToolSpecification(
						new Tool(SAVE_SCREENSHOT, "Take a screenshot and save to file",
										createSchema(Map.of(
														FILENAME, Map.of(TYPE, STRING, DESCRIPTION, "Filename to save (e.g., 'screenshot.png')"),
														FORMAT, Map.of(TYPE, STRING, DESCRIPTION, IMAGE_FORMAT),
														APP_ONLY, Map.of(TYPE, BOOLEAN, DESCRIPTION, "Take screenshot of app window only (default: false)")
										))),
						(exchange, arguments) -> {
							try {
								String filename = (String) arguments.get(FILENAME);
								String format = (String) arguments.getOrDefault(FORMAT, PNG);
								boolean appOnly = booleanParam(arguments, APP_ONLY, false);

								File file = new File(filename);
								BufferedImage screenshot = appOnly ?
												swingMcpServer.takeApplicationScreenshot() :
												swingMcpServer.takeScreenshot();
								SwingMcpServer.saveScreenshot(screenshot, file, format);

								exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
												.level(McpSchema.LoggingLevel.INFO)
												.logger(SCREENSHOT)
												.data("Screenshot saved to: " + file.getAbsolutePath())
												.build());

								return new CallToolResult(String.format(SCREENSHOT_DATA, file.getAbsolutePath(), appOnly), false);
							}
							catch (IOException e) {
								return new CallToolResult("Failed to save screenshot: " + e.getMessage(), true);
							}
						}
		));

		// Get screen size tool
		server.addTool(new SyncToolSpecification(
						new Tool(SCREEN_SIZE, "Get the current screen dimensions", TYPE_OBJECT),
						(exchange, arguments) -> {
							Dimension size = SwingMcpServer.getScreenSize();

							return new CallToolResult(String.format(SCREEN_SIZE_DATA, size.width, size.height), false);
						}
		));
	}

	private static void registerWindowTools(McpSyncServer server, SwingMcpServer swingMcpServer) {
		// Focus application window tool
		server.addTool(new SyncToolSpecification(
						new Tool(FOCUS_WINDOW, "Bring the application window to front and focus it", TYPE_OBJECT),
						(exchange, arguments) -> {
							swingMcpServer.focusWindow();

							return new CallToolResult("Application window focused", false);
						}
		));

		// Click at position tool
		server.addTool(new SyncToolSpecification(
						new Tool(CLICK_AT, "Click at specific screen coordinates",
										createSchema(Map.of(
														X, Map.of(TYPE, NUMBER, DESCRIPTION, "X coordinate"),
														Y, Map.of(TYPE, NUMBER, DESCRIPTION, "Y coordinate")
										))),
						(exchange, arguments) -> {
							int x = integerParam(arguments, X, 0);
							int y = integerParam(arguments, Y, 0);
							swingMcpServer.clickAt(x, y);

							exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
											.level(McpSchema.LoggingLevel.INFO)
											.logger(MOUSE)
											.data("Clicked at: " + x + ", " + y)
											.build());

							return new CallToolResult("Clicked at position", false);
						}
		));
	}

	// Helper methods
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

	private static String createSchema(Map<String, Map<String, String>> properties) {
		StringBuilder props = new StringBuilder();
		StringBuilder required = new StringBuilder();

		properties.forEach((name, prop) -> {
			if (props.length() > 0) {
				props.append(",");
			}
			props.append(String.format("""
							"%s": {
								"type": "%s",
								"description": "%s"
							}""", name, prop.get(TYPE), prop.get(DESCRIPTION)));

			// Add to required if not optional
			if (!"true".equals(prop.getOrDefault("optional", "false"))) {
				if (required.length() > 0) {
					required.append(",");
				}
				required.append("\"").append(name).append("\"");
			}
		});

		return String.format("""
										{
											"type": "object",
											"properties": {
												%s
											}%s
										}
										""", props,
						required.length() > 0 ? ",\n\"required\": [" + required + "]" : "");
	}
}
