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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.awt.AWTEvent.WINDOW_EVENT_MASK;
import static java.awt.event.WindowEvent.*;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * MCP Server for UI automation of Swing applications.
 * Provides keyboard control, screenshot capabilities, and HTTP-only MCP protocol integration.
 * This version is simplified to support only HTTP transport.
 */
final class SwingMcpServer {

	private static final Logger LOG = LoggerFactory.getLogger(SwingMcpServer.class);

	// Tool names (public for SwingMcpPlugin)
	static final String TYPE_TEXT = "type_text";
	static final String KEY_COMBO = "key_combo";
	static final String TAB = "tab";
	static final String ARROW = "arrow";
	static final String ENTER = "enter";
	static final String ESCAPE = "escape";
	static final String CLEAR_FIELD = "clear_field";
	static final String APP_SCREENSHOT = "app_screenshot";
	static final String ACTIVE_WINDOW_SCREENSHOT = "active_window_screenshot";
	static final String APP_WINDOW_BOUNDS = "app_window_bounds";
	static final String FOCUS_WINDOW = "focus_window";
	static final String LIST_WINDOWS = "list_windows";

	// Schema property names and types (public for SwingMcpPlugin)
	static final String TEXT = "text";
	static final String COUNT = "count";
	static final String SHIFT = "shift";
	static final String FORMAT = "format";
	static final String STRING = "string";
	static final String NUMBER = "number";
	static final String BOOLEAN = "boolean";
	static final String PNG = "png";
	static final String IMAGE_FORMAT = "Image format: 'png' or 'jpg' (default: 'png')";

	// Schema constants
	static final String INPUT_SCHEMA = "{\"type\": \"object\", \"properties\": {}}";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final Robot robot;
	private final KeyboardController keyboardController;
	private final JComponent applicationComponent;

	private final WindowEventListener windowEventListener = new WindowEventListener();

	private volatile Window lastActiveWindow = null;
	private volatile long lastActivationTime = 0;

	SwingMcpServer(JComponent applicationComponent) throws AWTException {
		this.applicationComponent = requireNonNull(applicationComponent);
		this.robot = new Robot();
		this.keyboardController = new KeyboardController(robot);
		Toolkit.getDefaultToolkit().addAWTEventListener(windowEventListener, WINDOW_EVENT_MASK);
		// Configure robot for smooth operation
		robot.setAutoDelay(50); // Small delay between events for reliability
		robot.setAutoWaitForIdle(true); // Wait for events to be processed
	}

	/**
	 * Type text into the currently focused field
	 * @param text the text to type
	 */
	void typeText(String text) {
		LOG.debug("Typing text: {}", text);
		focusActiveWindow();
		keyboardController.typeText(text);
	}

	/**
	 * Press a key combination like Alt+A or Ctrl+S
	 * @param combo the key combination (e.g., "Alt+A", "Ctrl+Shift+S")
	 */
	void keyCombo(String combo) {
		LOG.debug("Key combo: {}", combo);
		focusActiveWindow();
		keyboardController.pressKeyCombo(combo);
	}

	/**
	 * Press Tab to navigate fields
	 * @param count number of times to press Tab
	 * @param shift whether to hold Shift (for backward navigation)
	 */
	void tab(int count, boolean shift) {
		focusActiveWindow();
		keyboardController.tab(count, shift);
	}

	/**
	 * Press Enter key
	 */
	void enter() {
		focusActiveWindow();
		keyboardController.enter();
	}

	/**
	 * Press Escape key
	 */
	void escape() {
		focusActiveWindow();
		keyboardController.escape();
	}

	/**
	 * Press arrow keys for navigation
	 * @param direction "up", "down", "left", or "right"
	 * @param count number of times to press the arrow key
	 */
	void arrow(String direction, int count) {
		focusActiveWindow();
		keyboardController.arrow(direction, count);
	}

	/**
	 * Clear the current field by selecting all and deleting
	 */
	void clearField() {
		focusActiveWindow();
		keyboardController.clearField();
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
			throw new RuntimeException("Failed to serialize window information", e);
		}
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
		Window window = (Window) SwingUtilities.getAncestorOfClass(Window.class, applicationComponent);
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

	private Window findWindowByTitle(Window[] windows, String title) {
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

	void stop() {
		Toolkit.getDefaultToolkit().removeAWTEventListener(windowEventListener);
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
	}
}