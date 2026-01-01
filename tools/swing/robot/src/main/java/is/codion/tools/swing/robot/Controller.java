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
package is.codion.tools.swing.robot;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.KeyStroke;
import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.Robot;
import java.awt.Window;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.awt.Window.getWindows;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.lang.Character.isUpperCase;
import static java.util.Objects.requireNonNull;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * Keyboard controller with high-level operations, based on {@link Robot}
 */
public final class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

	private static final int DEFAULT_AUTO_DELAY = 50;
	private static final String PRESSED = "pressed ";

	private final Robot robot;

	private final Event<KeyStrokeDescription> keyStrokeEvent = Event.event();

	private Controller(GraphicsDevice device) throws AWTException {
		robot = new FocusedRobot(requireNonNull(device));
		robot.setAutoDelay(DEFAULT_AUTO_DELAY);
		robot.setAutoWaitForIdle(true);
	}

	/**
	 * @param autoDelay the robot auto delay in ms
	 * @see Robot#setAutoDelay(int)
	 */
	public void autoDelay(int autoDelay) {
		robot.setAutoDelay(autoDelay);
	}

	/**
	 * Types the given text.
	 * @param text the text to type
	 */
	public void type(String text) {
		type(text, null);
	}

	/**
	 * Types the given text.
	 * @param text the text to type
	 * @param description the description
	 */
	public void type(String text, @Nullable String description) {
		requireNonNull(text);
		LOG.debug("Typing text '{}', desc: '{}'", text, description);
		keyStrokeEvent.accept(new DefaultKeyStrokeDescription("\"" + text + "\"", 1, description));
		for (char character : text.toCharArray()) {
			typeCharacter(character);
		}
	}

	/**
	 * Processes the given keystroke.
	 * @param keyStroke the AWT formatted keystroke
	 * @see java.awt.AWTKeyStroke#getAWTKeyStroke(String)
	 */
	public void key(String keyStroke) {
		key(keyStroke, 1);
	}

	/**
	 * Processes the given keystroke.
	 * @param keyStroke the AWT formatted keystroke
	 * @param repeat the number of times to repeat the keystroke
	 * @see java.awt.AWTKeyStroke#getAWTKeyStroke(String)
	 */
	public void key(String keyStroke, int repeat) {
		key(keyStroke, repeat, null);
	}

	/**
	 * Processes the given keystroke.
	 * @param keyStroke the AWT formatted keystroke
	 * @param description the description
	 * @see java.awt.AWTKeyStroke#getAWTKeyStroke(String)
	 */
	public void key(String keyStroke, @Nullable String description) {
		key(keyStroke, 1, description);
	}

	/**
	 * Processes the given keystroke.
	 * @param keyStroke the AWT formatted keystroke
	 * @param repeat the number of times to repeat the keystroke
	 * @param description the description
	 * @see java.awt.AWTKeyStroke#getAWTKeyStroke(String)
	 */
	public void key(String keyStroke, int repeat, @Nullable String description) {
		KeyStroke parsed = getKeyStroke(requireNonNull(keyStroke));
		if (parsed == null) {
			throw new IllegalArgumentException("Invalid keystroke: '" + keyStroke +
							"'. Use AWT format like: 'ENTER', 'ctrl S', 'shift TAB', 'alt F4', 'typed a'");
		}
		key(parsed, repeat, description);
	}

	public void pause(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return an {@link Observer} notified each time a keystroke has been processed
	 */
	Observer<KeyStrokeDescription> key() {
		return keyStrokeEvent.observer();
	}

	/**
	 * @return a {@link Controller} for the default screen device
	 */
	public static Controller controller() {
		return controller(getLocalGraphicsEnvironment().getDefaultScreenDevice());
	}

	/**
	 * @param device the screen device
	 * @return a {@link Controller} for the given screen device
	 */
	public static Controller controller(GraphicsDevice device) {
		try {
			return new Controller(device);
		}
		catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}

	interface KeyStrokeDescription {

		String keyStroke();

		@Nullable String description();
	}

	private void key(KeyStroke keyStroke, int repeat, @Nullable String description) {
		requireNonNull(keyStroke);
		if (repeat < 1) {
			throw new IllegalArgumentException("Repeat value must be greater than zero");
		}
		keyStrokeEvent.accept(new DefaultKeyStrokeDescription(keyStroke.toString().replace(PRESSED, ""), repeat, description));
		LOG.debug("Processing keyStroke: '{}' repeat: {}", keyStroke, repeat);
		for (int i = 0; i < repeat; i++) {
			if (keyStroke.getKeyChar() != CHAR_UNDEFINED && keyStroke.getKeyCode() == 0) {
				typeCharacter(keyStroke.getKeyChar());// Handle "typed" keystrokes (e.g., "typed a", "typed !")
			}
			else {
				pressModifier(keyStroke.getModifiers());
				robot.keyPress(keyStroke.getKeyCode());
				robot.keyRelease(keyStroke.getKeyCode());
				releaseModifier(keyStroke.getModifiers());
			}
		}
	}

	private void typeCharacter(char character) {
		int keyCode = getExtendedKeyCodeForChar(character);
		if (CHAR_UNDEFINED == keyCode) {
			throw new RuntimeException("Cannot type character: " + character);
		}

		boolean needShift = isUpperCase(character) || "!@#$%^&*()_+{}|:\"<>?".indexOf(character) >= 0;
		if (needShift) {
			robot.keyPress(VK_SHIFT);
		}
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
		if (needShift) {
			robot.keyRelease(VK_SHIFT);
		}
	}

	private void pressModifier(int modifiers) {
		if ((modifiers & CTRL_DOWN_MASK) != 0) {
			robot.keyPress(VK_CONTROL);
		}
		if ((modifiers & ALT_DOWN_MASK) != 0) {
			robot.keyPress(VK_ALT);
		}
		if ((modifiers & SHIFT_DOWN_MASK) != 0) {
			robot.keyPress(VK_SHIFT);
		}
		if ((modifiers & META_DOWN_MASK) != 0) {
			robot.keyPress(VK_META);
		}
	}

	private void releaseModifier(int modifiers) {
		if ((modifiers & META_DOWN_MASK) != 0) {
			robot.keyRelease(VK_META);
		}
		if ((modifiers & SHIFT_DOWN_MASK) != 0) {
			robot.keyRelease(VK_SHIFT);
		}
		if ((modifiers & ALT_DOWN_MASK) != 0) {
			robot.keyRelease(VK_ALT);
		}
		if ((modifiers & CTRL_DOWN_MASK) != 0) {
			robot.keyRelease(VK_CONTROL);
		}
	}

	private final class FocusedRobot extends Robot {

		private static final int FOCUS_WAIT_DELAY = 1000;

		private FocusedRobot(GraphicsDevice device) throws AWTException {
			super(device);
		}

		@Override
		public synchronized void keyPress(int keycode) {
			ensureFocused();
			super.keyPress(keycode);
		}

		@Override
		public synchronized void keyRelease(int keycode) {
			ensureFocused();
			super.keyRelease(keycode);
		}

		private void ensureFocused() {
			while (!isJvmWindowFocused()) {
				LOG.debug("Waiting for JVM window focus");
				pause(FOCUS_WAIT_DELAY);
			}
		}

		private static boolean isJvmWindowFocused() {
			for (Window window : getWindows()) {
				if (window.isFocused()) {
					return true;
				}
			}

			return false;
		}
	}

	private static final class DefaultKeyStrokeDescription implements KeyStrokeDescription {

		private final String keyStroke;
		private final @Nullable String description;

		private DefaultKeyStrokeDescription(String keyStroke, int repeat, @Nullable String description) {
			this.keyStroke = keyStroke + (repeat > 1 ? " [" + repeat + "]" : "");
			this.description = description;
		}

		@Override
		public String keyStroke() {
			return keyStroke;
		}

		@Override
		public @Nullable String description() {
			return description;
		}
	}
}
