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
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.KeyboardFocusManager;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.Window.getWindows;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.lang.Character.isUpperCase;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * Keyboard controller with high-level operations. Delivers input either through a {@link Robot}
 * ({@link Transport#ROBOT}, realistic OS-level input, for demo recording) or by dispatching events
 * directly on the event dispatch thread ({@link Transport#EDT}, deterministic and synchronously
 * confirmed, for reliable programmatic control).
 */
public final class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

	private static final int DEFAULT_AUTO_DELAY = 50;
	private static final int DEFAULT_VERIFY_TIMEOUT = 500;
	private static final String PRESSED = "pressed ";

	/**
	 * The available input transports.
	 */
	public enum Transport {
		/**
		 * Realistic OS-level input via {@link Robot}, focus-dependent. Suited to demo recording.
		 */
		ROBOT,
		/**
		 * In-JVM input dispatched on the event dispatch thread, deterministic and synchronously confirmed.
		 * Suited to reliable programmatic control.
		 */
		EDT
	}

	private final Transport transport;
	private final @Nullable Robot robot;
	private final Verifier verifier = new Verifier();

	private final Event<KeyStrokeDescription> keyStrokeEvent = Event.event();

	private volatile int verifyTimeout = DEFAULT_VERIFY_TIMEOUT;

	private Controller(Transport transport, @Nullable GraphicsDevice device) throws AWTException {
		this.transport = transport;
		this.robot = transport == Transport.ROBOT ? createRobot(requireNonNull(device)) : null;
		addKeyEventListener();
	}

	private static Robot createRobot(GraphicsDevice device) throws AWTException {
		Robot robot = new FocusedRobot(device);
		robot.setAutoDelay(DEFAULT_AUTO_DELAY);
		robot.setAutoWaitForIdle(true);

		return robot;
	}

	/**
	 * The auto delay of the underlying {@link Robot}, no effect when using {@link Transport#EDT}.
	 * @param autoDelay the robot auto delay in ms
	 * @see Robot#setAutoDelay(int)
	 */
	public void autoDelay(int autoDelay) {
		if (robot != null) {
			robot.setAutoDelay(autoDelay);
		}
	}

	/**
	 * @param verifyTimeout the number of milliseconds to wait for the key event confirming an interaction
	 */
	public void verifyTimeout(int verifyTimeout) {
		this.verifyTimeout = verifyTimeout;
	}

	/**
	 * Types the given text.
	 * @param text the text to type
	 * @return the {@link Interaction} verdict for the typed text
	 */
	public Interaction type(String text) {
		return type(text, null);
	}

	/**
	 * Types the given text.
	 * @param text the text to type
	 * @param description the description
	 * @return the {@link Interaction} verdict for the typed text
	 */
	public Interaction type(String text, @Nullable String description) {
		requireNonNull(text);
		LOG.debug("Typing text '{}', desc: '{}'", text, description);
		keyStrokeEvent.accept(new DefaultKeyStrokeDescription("\"" + text + "\"", 1, description));
		Interaction interaction = null;
		for (char character : text.toCharArray()) {
			interaction = worst(interaction, verifier.verify(getKeyStroke(character),
							() -> character(character), verifyTimeout));
		}

		return interaction == null
						? new Interaction("\"" + text + "\"", Interaction.Delivery.CONSUMED, null, null)
						: new Interaction("\"" + text + "\"", interaction.delivery(), interaction.component(), null);
	}

	/**
	 * Processes the given keystroke.
	 * @param keyStroke the AWT formatted keystroke
	 * @return the {@link Interaction} verdict for the typed key
	 * @see java.awt.AWTKeyStroke#getAWTKeyStroke(String)
	 * @throws FocusLostException in case application input focus is lost
	 */
	public Interaction key(String keyStroke) {
		return key(keyStroke, 1);
	}

	/**
	 * Processes the given keystroke.
	 * @param keyStroke the AWT formatted keystroke
	 * @param repeat the number of times to repeat the keystroke
	 * @return the {@link Interaction} verdict for the typed key
	 * @see java.awt.AWTKeyStroke#getAWTKeyStroke(String)
	 * @throws FocusLostException in case application input focus is lost
	 */
	public Interaction key(String keyStroke, int repeat) {
		return key(keyStroke, repeat, null);
	}

	/**
	 * Processes the given keystroke.
	 * @param keyStroke the AWT formatted keystroke
	 * @param description the description
	 * @return the {@link Interaction} verdict for the keystroke
	 * @see java.awt.AWTKeyStroke#getAWTKeyStroke(String)
	 * @throws FocusLostException in case application input focus is lost
	 */
	public Interaction key(String keyStroke, @Nullable String description) {
		return key(keyStroke, 1, description);
	}

	/**
	 * Processes the given keystroke.
	 * @param keyStroke the AWT formatted keystroke
	 * @param repeat the number of times to repeat the keystroke
	 * @param description the description
	 * @return the {@link Interaction} verdict for the keystroke
	 * @see java.awt.AWTKeyStroke#getAWTKeyStroke(String)
	 * @throws FocusLostException in case application input focus is lost
	 */
	public Interaction key(String keyStroke, int repeat, @Nullable String description) {
		KeyStroke parsed = getKeyStroke(requireNonNull(keyStroke));
		if (parsed == null) {
			throw new IllegalArgumentException("Invalid keystroke: '" + keyStroke +
							"'. Use AWT format like: 'ENTER', 'ctrl S', 'shift TAB', 'alt F4', 'typed a'");
		}

		return key(parsed, repeat, description);
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
	 * @return a {@link Controller} for the given screen device, using the {@link Transport#ROBOT} transport
	 */
	public static Controller controller(GraphicsDevice device) {
		try {
			return new Controller(Transport.ROBOT, device);
		}
		catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param transport the input transport
	 * @return a {@link Controller} using the given transport, {@link Transport#ROBOT} uses the default screen device
	 */
	public static Controller controller(Transport transport) {
		try {
			return new Controller(requireNonNull(transport),
							transport == Transport.ROBOT ? getLocalGraphicsEnvironment().getDefaultScreenDevice() : null);
		}
		catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Thrown when an input action is attempted when no application window is focused.
	 */
	public static final class FocusLostException extends RuntimeException {

		private FocusLostException() {}
	}

	interface KeyStrokeDescription {

		String keyStroke();

		@Nullable String description();
	}

	private void addKeyEventListener() {
		KeyboardFocusManager focusManager = getCurrentKeyboardFocusManager();
		focusManager.addKeyEventDispatcher(e -> {
			verifier.dispatched(e);
			return false;
		});
		focusManager.addKeyEventPostProcessor(e -> {
			verifier.postProcessed(e);
			return false;
		});
	}

	private Interaction key(KeyStroke keyStroke, int repeat, @Nullable String description) {
		requireNonNull(keyStroke);
		if (repeat < 1) {
			throw new IllegalArgumentException("Repeat value must be greater than zero");
		}
		keyStrokeEvent.accept(new DefaultKeyStrokeDescription(keyStroke.toString().replace(PRESSED, ""), repeat, description));
		LOG.debug("Processing keyStroke: '{}' repeat: {}", keyStroke, repeat);
		Interaction interaction = null;
		for (int i = 0; i < repeat; i++) {
			interaction = worst(interaction, verifier.verify(keyStroke, () -> inject(keyStroke), verifyTimeout));
		}

		return interaction;
	}

	private void inject(KeyStroke keyStroke) {
		if (keyStroke.getKeyChar() != CHAR_UNDEFINED && keyStroke.getKeyCode() == 0) {
			character(keyStroke.getKeyChar());// Handle "typed" keystrokes (e.g., "typed a", "typed !")
		}
		else if (transport == Transport.EDT) {
			dispatch(keyStroke.getModifiers(), keyStroke.getKeyCode());
		}
		else {
			pressModifier(keyStroke.getModifiers());
			robot.keyPress(keyStroke.getKeyCode());
			robot.keyRelease(keyStroke.getKeyCode());
			releaseModifier(keyStroke.getModifiers());
		}
	}

	private void character(char character) {
		if (transport == Transport.EDT) {
			dispatchTyped(character);
		}
		else {
			typeCharacter(character);
		}
	}

	/**
	 * Posts a keystroke to the event queue targeting the current focus owner and waits for it to be
	 * dispatched, so the interaction is confirmed synchronously. Posting, rather than dispatching directly,
	 * means any exception thrown while processing the event is handled by the application's event dispatch
	 * thread just as it would be for real input, instead of propagating back here.
	 */
	private static void dispatch(int modifiers, int keyCode) {
		onEventDispatchThread(() -> {
			Component focusOwner = focusOwner();
			long when = currentTimeMillis();
			EventQueue eventQueue = getDefaultToolkit().getSystemEventQueue();
			eventQueue.postEvent(new KeyEvent(focusOwner, KEY_PRESSED, when, modifiers, keyCode, CHAR_UNDEFINED));
			eventQueue.postEvent(new KeyEvent(focusOwner, KEY_RELEASED, when, modifiers, keyCode, CHAR_UNDEFINED));
		});
		barrier();
	}

	private static void dispatchTyped(char character) {
		onEventDispatchThread(() -> {
			Component focusOwner = focusOwner();
			getDefaultToolkit().getSystemEventQueue()
							.postEvent(new KeyEvent(focusOwner, KEY_TYPED, currentTimeMillis(), 0, VK_UNDEFINED, character));
		});
		barrier();
	}

	/**
	 * Waits for events already posted on the event queue to be dispatched, by running an empty task behind them.
	 */
	private static void barrier() {
		onEventDispatchThread(() -> {});
	}

	private static void onEventDispatchThread(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		}
		else {
			try {
				SwingUtilities.invokeAndWait(runnable);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			catch (InvocationTargetException e) {
				if (e.getCause() instanceof RuntimeException) {
					throw (RuntimeException) e.getCause();
				}
				throw new RuntimeException(e.getCause());
			}
		}
	}

	private static Component focusOwner() {
		KeyboardFocusManager focusManager = getCurrentKeyboardFocusManager();
		Component focusOwner = focusManager.getFocusOwner();
		if (focusOwner == null) {
			Window focusedWindow = focusManager.getFocusedWindow();
			if (focusedWindow != null) {
				focusOwner = focusedWindow.getMostRecentFocusOwner();
			}
		}
		if (focusOwner == null) {
			throw new FocusLostException();
		}

		return focusOwner;
	}

	/**
	 * Combines two interactions for a repeated keystroke, keeping the worst delivery and the latest details.
	 */
	private static Interaction worst(@Nullable Interaction previous, Interaction interaction) {
		if (previous == null) {
			return interaction;
		}
		Interaction.Delivery delivery = previous.delivery().ordinal() >= interaction.delivery().ordinal()
						? previous.delivery() : interaction.delivery();

		return new Interaction(interaction.keyStroke(), delivery,
						interaction.component() == null ? previous.component() : interaction.component(),
						interaction.action() == null ? previous.action() : interaction.action());
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

	private static final class FocusedRobot extends Robot {

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
			super.keyRelease(keycode);// Make sure we don't leave a key pressed, in case we terminate due to focus loss
			ensureFocused();
		}

		private static void ensureFocused() {
			for (Window window : getWindows()) {
				if (window.isFocused()) {
					return;
				}
			}

			throw new FocusLostException();
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
