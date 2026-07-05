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
package is.codion.tools.swing.robot;

import is.codion.tools.swing.robot.Interaction.Delivery;

import org.jspecify.annotations.Nullable;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Correlates a requested {@link KeyStroke} with the key events observed on the
 * {@link java.awt.KeyboardFocusManager}, producing an {@link Interaction} verdict. Robot-free, so the
 * matching and verdict logic is exercised in isolation, {@link #dispatched(KeyEvent)} and
 * {@link #postProcessed(KeyEvent)} are fed by the controller's focus manager listeners.
 */
final class Verifier {

	private static final String PRESSED = "pressed ";

	private final AtomicReference<@Nullable Expectation> expectation = new AtomicReference<>();

	/**
	 * Arms an expectation for the given keystroke, runs the injection and waits for the matching event.
	 * @param keyStroke the keystroke being injected
	 * @param inject injects the keystroke
	 * @param timeout the number of milliseconds to wait for the matching event
	 * @return the interaction verdict
	 */
	Interaction verify(KeyStroke keyStroke, Runnable inject, int timeout) {
		Expectation armed = new Expectation(keyStroke);
		expectation.set(armed);
		try {
			inject.run();
			boolean matched = armed.latch.await(timeout, MILLISECONDS);
			Delivery delivery = matched ? (armed.consumed ? Delivery.CONSUMED : Delivery.FELL_THROUGH)
							: armed.dispatched ? Delivery.CONSUMED : Delivery.MISSED;

			return new Interaction(label(keyStroke), delivery, armed.component, armed.action);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		finally {
			expectation.set(null);
		}
	}

	/**
	 * Called as each key event enters dispatch, before the focus owner processes it.
	 * @param event the event
	 */
	void dispatched(KeyEvent event) {
		Expectation armed = expectation.get();
		if (armed != null && matches(armed.keyStroke, event)) {
			armed.dispatched = true;
		}
	}

	/**
	 * Called after the focus owner has processed the event, when {@link KeyEvent#isConsumed()} is authoritative.
	 * @param event the event
	 */
	void postProcessed(KeyEvent event) {
		Expectation armed = expectation.get();
		if (armed != null && armed.latch.getCount() > 0 && matches(armed.keyStroke, event)) {
			armed.consumed = event.isConsumed();
			armed.component = describe(event.getComponent());
			armed.action = resolveAction(event.getComponent(), armed.keyStroke);
			armed.latch.countDown();
		}
	}

	static boolean matches(KeyStroke keyStroke, KeyEvent event) {
		if (event.getID() == KeyEvent.KEY_TYPED) {
			// A typed character carries incidental modifiers (shift for uppercase and symbols),
			// so match on the character itself rather than the modifier-sensitive keystroke.
			return keyStroke.getKeyCode() == KeyEvent.VK_UNDEFINED
							&& keyStroke.getKeyChar() == event.getKeyChar();
		}

		return keyStroke.equals(KeyStroke.getKeyStrokeForEvent(event));
	}

	private static String label(KeyStroke keyStroke) {
		return keyStroke.toString().replace(PRESSED, "");
	}

	private static @Nullable String describe(@Nullable Component component) {
		if (component == null) {
			return null;
		}
		String name = component.getName();

		return component.getClass().getSimpleName() + (name == null ? "" : "[" + name + "]");
	}

	private static @Nullable String resolveAction(@Nullable Component component, KeyStroke keyStroke) {
		if (!(component instanceof JComponent)) {
			return null;
		}
		JComponent jComponent = (JComponent) component;
		Object actionMapKey = actionMapKey(jComponent, JComponent.WHEN_FOCUSED, keyStroke);
		if (actionMapKey == null) {
			actionMapKey = actionMapKey(jComponent, JComponent.WHEN_IN_FOCUSED_WINDOW, keyStroke);
		}
		Container container = jComponent;
		while (actionMapKey == null && container != null) {
			if (container instanceof JComponent) {
				actionMapKey = actionMapKey((JComponent) container, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keyStroke);
			}
			container = container.getParent();
		}

		return actionMapKey == null ? null : actionMapKey.toString();
	}

	private static @Nullable Object actionMapKey(JComponent component, int condition, KeyStroke keyStroke) {
		InputMap inputMap = component.getInputMap(condition);

		return inputMap == null ? null : inputMap.get(keyStroke);
	}

	private static final class Expectation {

		private final KeyStroke keyStroke;
		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile boolean dispatched;
		private volatile boolean consumed;
		private volatile @Nullable String component;
		private volatile @Nullable String action;

		private Expectation(KeyStroke keyStroke) {
			this.keyStroke = keyStroke;
		}
	}
}
