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
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.tools.swing.mcp.SwingMcpHttpServer.HttpTool;
import is.codion.tools.swing.robot.Controller;
import is.codion.tools.swing.robot.Controller.FocusLostException;
import is.codion.tools.swing.robot.Narrator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.integerValue;
import static java.awt.AWTEvent.WINDOW_EVENT_MASK;
import static java.awt.event.WindowEvent.*;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;

/**
 * MCP Server for UI automation of Swing applications.
 * Provides keyboard control, screenshot capabilities, and HTTP-only MCP protocol integration.
 * <p>
 * The HTTP server runs in-process and can be toggled on/off at runtime.
 * Claude Desktop connects via a Java bridge that translates STDIO to HTTP.
 * <p>
 * Configure with system properties:
 * -Dcodion.swing.mcp.http.port=8080      (HTTP server port, default 8080)
 * @see #builder()
 */
public final class SwingMcpServer {

	private static final Logger LOG = LoggerFactory.getLogger(SwingMcpServer.class);

	/**
	 * System property to set the HTTP server port (default: 8080).
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 8080
	 * </ul>
	 */
	public static final PropertyValue<Integer> HTTP_PORT = integerValue("codion.swing.mcp.http.port", 8080);

	// Tool names
	static final String TYPE_TEXT = "type_text";
	static final String KEY_COMBO = "key";
	static final String CLEAR_FIELD = "clear_field";
	static final String APP_SCREENSHOT = "app_screenshot";
	static final String ACTIVE_WINDOW_SCREENSHOT = "active_window_screenshot";
	static final String APP_WINDOW_BOUNDS = "app_window_bounds";
	static final String FOCUS_WINDOW = "focus_window";

	static final String TEXT = "text";
	static final String FORMAT = "format";
	static final String STRING = "string";
	static final String PNG = "png";
	static final String IMAGE_FORMAT = "Image format: 'png' or 'jpg' (default: 'png')";

	// Schema constants
	static final String INPUT_SCHEMA = "{\"type\": \"object\", \"properties\": {}}";

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

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final Supplier<Window> applicationWindow;
	private final boolean includeNarrator;

	private ExecutorService executor;
	private Controller controller;
	private Narrator narrator;
	private SwingMcpHttpServer httpServer;
	private WindowEventListener windowEventListener;

	private volatile Window lastActiveWindow = null;
	private volatile long lastActivationTime = 0;

	private SwingMcpServer(Builder builder) {
		this.applicationWindow = builder.window;
		this.includeNarrator = builder.narrator;
	}

	SwingMcpServer(Supplier<Window> applicationWindow, boolean includeNarrator) {
		this.applicationWindow = requireNonNull(applicationWindow);
		this.includeNarrator = includeNarrator;
		initializeServer();
	}

	/**
	 * Type text into the currently focused field
	 * @param text the text to type
	 * @throws FocusLostException in case the application has lost the input focus
	 */
	void type(String text) {
		LOG.debug("Typing text: {}", text);
		focusActiveWindow();
		controller.type(text);
	}

	/**
	 * Press a key combination using AWT KeyStroke format.
	 * Examples: "ENTER", "TAB", "ctrl S", "shift TAB", "alt F4", "UP", "typed a"
	 * @param keystroke the key combination in AWT KeyStroke format
	 * @param repeat the number of times to repeat the keystroke
	 * @param description a description of the action associated with the keystroke
	 * @throws FocusLostException in case the application has lost the input focus
	 */
	void key(String keystroke, int repeat, @Nullable String description) {
		LOG.debug("Key combo: {}", keystroke);
		focusActiveWindow();
		controller.key(keystroke, repeat, description);
	}

	/**
	 * Clear the current field by selecting all and deleting
	 * @throws FocusLostException in case the application has lost the input focus
	 */
	void clearField() {
		focusActiveWindow();
		controller.key("ctrl A");
		controller.key("DELETE");
	}

	/**
	 * @return true if a narrator is available
	 */
	boolean narratorAvailable() {
		return narrator != null;
	}

	/**
	 * @param text the text to narrate
	 * @return true if a narrator was available
	 */
	boolean narrate(String text) {
		if (narrator != null) {
			narrator.narrate(text);

			return true;
		}

		return false;
	}

	/**
	 * @return true if a narrator was available
	 */
	boolean clearNarration() {
		if (narrator != null) {
			narrator.clearNarration();

			return true;
		}

		return false;
	}

	/**
	 * @return true if a narrator was available
	 */
	boolean clearKeyStrokes() {
		if (narrator != null) {
			narrator.clearKeyStrokes();

			return true;
		}

		return false;
	}

	/**
	 * Get the bounds of the application window
	 * @return Rectangle with x, y, width, height of the application window
	 */
	Rectangle getApplicationWindowBounds() {
		return getApplicationWindow().getBounds();
	}

	/**
	 * Take a screenshot of just the application window by painting it directly into a BufferedImage.
	 * This method works regardless of whether the window is visible or obscured by other windows.
	 * @return the screenshot as a BufferedImage
	 */
	BufferedImage takeApplicationScreenshot() {
		return paintWindowToImage(getApplicationWindow());
	}

	/**
	 * Take a screenshot of just the active window by painting it directly into a BufferedImage.
	 * This method works regardless of whether the window is visible or obscured by other windows.
	 * @return the screenshot as a BufferedImage
	 */
	BufferedImage takeActiveWindowScreenshot() {
		return paintWindowToImage(getActiveWindow());
	}

	/**
	 * Bring the application window to front (public method for MCP tools)
	 */
	void focusWindow() {
		focusApplicationWindow();
	}

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
		if (narrator != null) {
			narrator.close();
			narrator = null;
		}
		if (windowEventListener != null) {
			Toolkit.getDefaultToolkit().removeAWTEventListener(windowEventListener);
			windowEventListener = null;
		}
		controller = null;
		LOG.info(SERVER_STOPPED_INFO);
	}

	private void runServer() {
		try {
			initializeServer();
			startHttpServer();
		}
		catch (Exception e) {
			LOG.error("Failed to start MCP server: {}", e.getMessage());
		}
	}

	private void initializeServer() {
		Window window = applicationWindow.get();
		this.controller = window == null ? Controller.controller() : Controller.controller(window.getGraphicsConfiguration().getDevice());
		this.narrator = includeNarrator ? Narrator.narrator(controller) : null;
		this.windowEventListener = new WindowEventListener();
		Toolkit.getDefaultToolkit().addAWTEventListener(windowEventListener, WINDOW_EVENT_MASK);
	}

	private void startHttpServer() throws IOException {
		httpServer = new SwingMcpHttpServer(HTTP_PORT.getOrThrow(), CODION_SWING_MCP, Version.versionString());
		registerHttpTools(httpServer);
		httpServer.start();
		LOG.info(SERVER_STARTUP_INFO);
	}

	private void registerHttpTools(SwingMcpHttpServer httpServer) {
		// Type text tool
		httpServer.addTool(new HttpTool(
						TYPE_TEXT, "Type text into the currently focused field",
						createSchema(TEXT, STRING, "The text to type"),
						arguments -> {
							String text = (String) arguments.get(TEXT);
							type(text);

							return "Text typed successfully";
						}
		));

		// Key combination tool - handles all keyboard input
		httpServer.addTool(new HttpTool(
						KEY_COMBO, "Press a key combination using AWT KeyStroke format",
						KEY_SCHEMA,
						arguments -> {
							String combo = (String) arguments.get("combo");
							int repeat = integerParam(arguments, "repeat", 1);
							String description = (String) arguments.get("description");
							key(combo, repeat, description);

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
						createSchema(FORMAT, STRING, IMAGE_FORMAT + " (tip: use 'jpg' for better compression)"),
						arguments -> screenshotToBase64(arguments, takeApplicationScreenshot())
		));

		// Active window screenshot tool
		httpServer.addTool(new HttpTool(
						ACTIVE_WINDOW_SCREENSHOT, "Take a screenshot of the currently active window (dialog, popup, etc.) and return as base64",
						createSchema(FORMAT, STRING, IMAGE_FORMAT + " (tip: use 'jpg' for better compression)"),
						arguments -> screenshotToBase64(arguments, takeActiveWindowScreenshot())
		));

		// Application window bounds tool
		httpServer.addTool(new HttpTool(
						APP_WINDOW_BOUNDS, "Get the application window bounds (x, y, width, height)",
						INPUT_SCHEMA,
						arguments -> {
							Rectangle bounds = getApplicationWindowBounds();

							return Map.of(
											"x", bounds.x,
											"y", bounds.y,
											WIDTH, bounds.width,
											HEIGHT, bounds.height);
						}
		));

		// Focus application window tool
		httpServer.addTool(new HttpTool(
						FOCUS_WINDOW, "Bring the application window to front and focus it",
						INPUT_SCHEMA,
						arguments -> {
							focusWindow();

							return "Application window focused";
						}
		));

		// Clear field tool
		httpServer.addTool(new HttpTool(
						CLEAR_FIELD, "Clear the current field by selecting all and deleting",
						INPUT_SCHEMA,
						arguments -> {
							clearField();

							return "Field cleared";
						}
		));

		// Narrator tools (only added if narrator is available)
		if (narratorAvailable()) {
			// Narrate tool
			httpServer.addTool(new HttpTool(
							"narrate", "Add narration text to the narrator window",
							createSchema(TEXT, STRING, "The narration text to display"),
							arguments -> {
								String text = (String) arguments.get(TEXT);
								if (narrate(text)) {
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
								if (clearNarration()) {
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
								if (clearKeyStrokes()) {
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
			String base64 = screenshotToBase64(screenshot, format);

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

	private List<WindowInfo> getApplicationWindows() {
		Window mainWindow = getApplicationWindow();
		Window[] allWindows = Window.getWindows();
		Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();

		return Arrays.stream(allWindows)
						.filter(Window::isVisible)
						.map(window -> {
							String title = getWindowTitle(window);
							String type = getWindowType(window);
							boolean isMainWindow = window == mainWindow;
							boolean isFocused = window == focusedWindow;
							boolean isActive = window.isActive();
							boolean isModal = window instanceof Dialog && ((Dialog) window).isModal();
							Rectangle bounds = window.getBounds();

							// Get parent window for dialogs
							String parentWindowTitle = null;
							if (window instanceof Dialog) {
								Window owner = window.getOwner();
								if (owner != null && owner.isVisible()) {
									parentWindowTitle = getWindowTitle(owner);
								}
							}

							return new WindowInfo(title, type, isMainWindow, isFocused,
											isActive, isModal, true, // visible
											window.isFocusableWindow(), // focusable
											new WindowBounds(bounds.x, bounds.y, bounds.width, bounds.height),
											parentWindowTitle);
						})
						.collect(toList());
	}

	/**
	 * Get information about all application windows
	 * @return JSON string containing window information
	 * @throws RuntimeException if JSON serialization fails
	 */
	String listWindows() {
		try {
			return OBJECT_MAPPER.writeValueAsString(new WindowListResponse(getApplicationWindows()));
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to serialize window information", e);
		}
	}

	private static BufferedImage paintWindowToImage(Window window) {
		BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			window.paint(graphics);

			return image;
		}
		finally {
			graphics.dispose();
		}
	}

	private static String getWindowTitle(Window window) {
		if (window instanceof JFrame) {
			return ((JFrame) window).getTitle();
		}
		else if (window instanceof JDialog) {
			return ((JDialog) window).getTitle();
		}

		return window.getClass().getSimpleName();
	}

	private static String getWindowType(Window window) {
		if (window instanceof JFrame) {
			return "frame";
		}
		else if (window instanceof JDialog) {
			return "dialog";
		}

		return "window";
	}

	private Window getApplicationWindow() {
		Window window = applicationWindow.get();
		if (window == null) {
			throw new IllegalStateException("No application window found");
		}

		return window;
	}

	private Window getActiveWindow() {
		// First priority: event-driven last active window
		if (lastActiveWindow != null && lastActiveWindow.isVisible() && lastActiveWindow.isFocusableWindow()) {
			LOG.debug("Using event-driven last active window: {} (activated at {})", getWindowTitle(lastActiveWindow), lastActivationTime);

			return lastActiveWindow;
		}

		// Fallback to existing window detection logic
		List<WindowInfo> windowInfos = getApplicationWindows();
		Window[] allWindows = Window.getWindows();

		// Second priority: focused window (when app is in foreground)
		for (WindowInfo info : windowInfos) {
			if (info.focused()) {
				Window window = findWindowByTitle(allWindows, info.title());
				if (window != null) {
					LOG.debug("Found focused window: {}", info.title());

					return window;
				}
			}
		}

		// Third priority: active modal dialog
		for (WindowInfo info : windowInfos) {
			if (info.active() && info.modal()) {
				Window window = findWindowByTitle(allWindows, info.title());
				if (window != null) {
					LOG.debug("Found active modal dialog: {}", info.title());

					return window;
				}
			}
		}

		// Fourth priority: any active window
		for (WindowInfo info : windowInfos) {
			if (info.active()) {
				Window window = findWindowByTitle(allWindows, info.title());
				if (window != null) {
					LOG.debug("Found active window: {}", info.title());

					return window;
				}
			}
		}

		// Fifth priority: when app is in background, prefer modal dialogs
		for (WindowInfo info : windowInfos) {
			if (info.modal() && info.visible() && info.focusable()) {
				Window window = findWindowByTitle(allWindows, info.title());
				if (window != null) {
					LOG.debug("Found modal dialog (app in background): {}", info.title());

					return window;
				}
			}
		}

		// Sixth priority: topmost non-main window (likely a dialog or lookup table)
		for (WindowInfo info : windowInfos) {
			if (!info.mainWindow() && info.visible() && info.focusable()) {
				Window window = findWindowByTitle(allWindows, info.title());
				if (window != null) {
					LOG.debug("Found topmost non-main window: {}", info.title());

					return window;
				}
			}
		}

		// Final fallback: main application window
		LOG.debug("No suitable window found, falling back to main application window");
		return getApplicationWindow();
	}

	private static Window findWindowByTitle(Window[] windows, String title) {
		for (Window window : windows) {
			if (window.isVisible() && title.equals(getWindowTitle(window))) {
				return window;
			}
		}
		return null;
	}

	/**
	 * Bring the application window to front and ensure it has focus
	 */
	private void focusApplicationWindow() {
		safeWindowOperation(() -> getApplicationWindow().toFront(), "Could not focus application window");
	}

	private void focusActiveWindow() {
		safeWindowOperation(() -> getActiveWindow().toFront(), "Could not focus active window");
	}

	private static void safeWindowOperation(Runnable operation, String errorMessage) {
		try {
			operation.run();
		}
		catch (Exception e) {
			LOG.warn(errorMessage, e);
			// Continue anyway - the action might still work
		}
	}

	/**
	 * Convert a screenshot to Base64 for transmission over MCP with compression and optional scaling
	 * @param image the image to convert
	 * @param format the image format (e.g., "png", "jpg")
	 * @return Base64 encoded string
	 * @throws IOException if conversion fails
	 */
	static String screenshotToBase64(BufferedImage image, String format) throws IOException {
		// Scale down large images to reduce context cost while maintaining readability
		BufferedImage processedImage = scaleImageIfNeeded(image);

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			if ("png".equalsIgnoreCase(format)) {
				writePngWithCompression(processedImage, baos);
			}
			else if ("jpg".equalsIgnoreCase(format)) {
				writeJpegWithUICompression(processedImage, baos);
			}
			else {
				// For other formats, use standard ImageIO with best quality
				ImageIO.write(processedImage, format, baos);
			}

			byte[] imageBytes = baos.toByteArray();

			// Log compression effectiveness
			if (processedImage != image) {
				LOG.debug("Screenshot scaled from {}x{} to {}x{}, size: {} bytes",
								image.getWidth(), image.getHeight(),
								processedImage.getWidth(), processedImage.getHeight(),
								imageBytes.length);
			}
			else {
				LOG.debug("Screenshot {}x{}, size: {} bytes",
								image.getWidth(), image.getHeight(), imageBytes.length);
			}

			return Base64.getEncoder().encodeToString(imageBytes);
		}
	}

	/**
	 * Scale down image if it's larger than optimal size for AI processing.
	 * Swing UIs remain readable even at lower resolutions.
	 */
	private static BufferedImage scaleImageIfNeeded(BufferedImage original) {
		final int maxWidth = 1024;  // Good balance between readability and size
		final int maxHeight = 768;

		if (original.getWidth() <= maxWidth && original.getHeight() <= maxHeight) {
			return original;
		}

		// Calculate scale factor to fit within max dimensions
		double scaleX = (double) maxWidth / original.getWidth();
		double scaleY = (double) maxHeight / original.getHeight();
		double scale = Math.min(scaleX, scaleY);

		int newWidth = (int) (original.getWidth() * scale);
		int newHeight = (int) (original.getHeight() * scale);

		BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		scaled.createGraphics().drawImage(original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);

		return scaled;
	}

	/**
	 * Write PNG with optimizations for Swing UI screenshots.
	 * Reduces color depth for better compression since Swing UIs typically use limited colors.
	 */
	private static void writePngWithCompression(BufferedImage image, ByteArrayOutputStream baos) throws IOException {
		// Convert to indexed color for better compression on UI screenshots
		BufferedImage optimized = optimizeForUI(image);
		ImageIO.write(optimized, "png", baos);
	}

	/**
	 * Optimize image for UI screenshots by reducing color depth.
	 * Swing UIs typically use a limited color palette, so this maintains quality while improving compression.
	 */
	private static BufferedImage optimizeForUI(BufferedImage original) {
		// For small images or already optimized images, return as-is
		if (original.getWidth() * original.getHeight() < 100000) { // ~316x316 pixels
			return original;
		}

		// Create a new image with reduced color precision for better PNG compression
		// This works well for Swing UIs which have solid colors and limited palettes
		BufferedImage optimized = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = optimized.createGraphics();

		// Copy the original image
		g2d.drawImage(original, 0, 0, null);
		g2d.dispose();

		return optimized;
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

	// Helper methods for HTTP tool creation
	static String createSchema(String propName, String propType, String propDesc) {
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

	static String createTwoPropertySchema(String prop1Name, String prop1Type, String prop1Desc,
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

	/**
	 * Write JPEG with compression optimized for UI screenshots.
	 * Maintains color fidelity for status indicators while achieving good compression.
	 */
	private static void writeJpegWithUICompression(BufferedImage image, ByteArrayOutputStream baos) throws IOException {
		ImageWriter writer = null;
		ImageOutputStream ios = null;
		try {
			// Get JPEG writer
			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
			if (!writers.hasNext()) {
				ImageIO.write(image, "jpeg", baos);
				return;
			}

			writer = writers.next();
			ios = ImageIO.createImageOutputStream(baos);
			writer.setOutput(ios);

			ImageWriteParam writeParam = writer.getDefaultWriteParam();
			if (writeParam.canWriteCompressed()) {
				writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				writeParam.setCompressionQuality(0.85f);
				LOG.debug("JPEG compression enabled with quality 0.85 for UI screenshot");
			}

			BufferedImage rgbImage = ensureRgbFormat(image);
			writer.write(null, new IIOImage(rgbImage, null, null), writeParam);
		}
		finally {
			if (writer != null) {
				writer.dispose();
			}
			if (ios != null) {
				ios.close();
			}
		}
	}

	/**
	 * Ensure image is in RGB format for JPEG (removes alpha channel if present).
	 * Uses white background which is common for Codion UI themes.
	 */
	private static BufferedImage ensureRgbFormat(BufferedImage original) {
		if (original.getType() == BufferedImage.TYPE_INT_RGB) {
			return original;
		}

		// Convert to RGB format with white background
		BufferedImage rgbImage = new BufferedImage(
						original.getWidth(),
						original.getHeight(),
						BufferedImage.TYPE_INT_RGB
		);

		Graphics2D g2d = rgbImage.createGraphics();
		// White background for transparency (matches typical Codion themes)
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
		// Draw original image on top
		g2d.drawImage(original, 0, 0, null);
		g2d.dispose();

		return rgbImage;
	}

	/**
	 * @return a {@link Builder.Factory} instance.
	 */
	public static Builder.Factory builder() {
		return Builder.FACTORY;
	}

	/**
	 * Builds a {@link State} for controlling a {@link SwingMcpServer}
	 */
	public static final class Builder {

		private static final Factory FACTORY = new Factory();

		private final Supplier<Window> window;

		private boolean narrator;
		private boolean start;

		private Builder(Supplier<Window> window) {
			this.window = window;
		}

		/**
		 * Default false.
		 * @param narrator if true a {@link is.codion.tools.swing.robot.Narrator} is included
		 * @return this builder
		 */
		public Builder narrator(boolean narrator) {
			this.narrator = narrator;
			return this;
		}

		/**
		 * Default false.
		 * @param start true if the server should be started right away
		 * @return this builder
		 */
		public Builder start(boolean start) {
			this.start = start;
			return this;
		}

		/**
		 * @return a {@link State} controlling the started state of the mcp server
		 */
		public State build() {
			ServerController controller = new ServerController(new SwingMcpServer(this));
			if (start) {
				controller.accept(true);
			}

			return State.builder()
							.value(start)
							.consumer(controller)
							.build();
		}

		public static final class Factory {

			private Factory() {}

			public Builder component(JComponent component) {
				requireNonNull(component);

				return window(() -> Ancestor.window().of(component).get());
			}

			public Builder window(Supplier<Window> window) {
				return new Builder(requireNonNull(window));
			}
		}
	}

	private static final class ServerController implements Consumer<Boolean> {

		private final SwingMcpServer server;

		private ServerController(SwingMcpServer server) {
			this.server = server;
		}

		@Override
		public void accept(Boolean start) {
			if (start) {
				server.start();
			}
			else {
				server.stop();
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

	// Data classes for JSON serialization
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record WindowInfo(String title, String type, boolean mainWindow, boolean focused,
										boolean active, boolean modal, boolean visible, boolean focusable, WindowBounds bounds,
										@JsonProperty("parentWindow") String parentWindow) {}

	record WindowBounds(int x, int y, int width, int height) {}

	record WindowListResponse(List<WindowInfo> windows) {}

	private final class WindowEventListener implements AWTEventListener {

		@Override
		public void eventDispatched(AWTEvent event) {
			onWindowEvent((WindowEvent) event);
		}

		private void onWindowEvent(WindowEvent event) {
			if (event.getID() == WINDOW_ACTIVATED || event.getID() == WINDOW_GAINED_FOCUS) {
				Window window = event.getWindow();
				if (window.isVisible() && window.isFocusableWindow()) {
					lastActiveWindow = window;
					lastActivationTime = System.currentTimeMillis();
					LOG.debug("Window activated/focused: {} at {}", getWindowTitle(window), lastActivationTime);
				}
			}
			else if (event.getID() == WINDOW_CLOSED) {
				Window window = event.getWindow();
				if (window == lastActiveWindow) {
					lastActiveWindow = null;
					lastActivationTime = 0;
					LOG.debug("Last active window closed: {}", getWindowTitle(window));
				}
			}
		}
	}
}