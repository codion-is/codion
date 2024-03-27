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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.key;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.awt.event.ActionEvent;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_ENTER;
import static javax.swing.JComponent.WHEN_FOCUSED;

/**
 * A utility class for adding focus traversal events based on the Enter key.
 */
public final class TransferFocusOnEnter {

	private TransferFocusOnEnter() {}

	/**
	 * Adds a key event to the component which transfers focus on enter, and backwards if SHIFT is down.
	 * Note that for JTextArea CTRL is added to move focus forward.
	 * @param component the component
	 * @param <T> the component type
	 * @return the component
	 * @see #disable(JComponent)
	 */
	public static <T extends JComponent> T enable(T component) {
		forwardBuilder(component).enable(component);
		backwardsBuilder().enable(component);

		return component;
	}

	/**
	 * Disables the transfer focus action added via {@link #enable(JComponent)}
	 * @param component the component
	 * @param <T> the component type
	 * @return the component
	 */
	public static <T extends JComponent> T disable(T component) {
		forwardBuilder(component).disable(component);
		backwardsBuilder().disable(component);

		return component;
	}

	/**
	 * Instantiates an Action for transferring keyboard focus forward.
	 * @return an Action for transferring focus
	 */
	public static Action forwardAction() {
		return new TransferFocusAction(false);
	}

	/**
	 * Instantiates an Action for transferring keyboard focus backward.
	 * @return an Action for transferring focus
	 */
	public static Action backwardAction() {
		return new TransferFocusAction(true);
	}

	private static KeyEvents.Builder backwardsBuilder() {
		return KeyEvents.builder(VK_ENTER)
						.modifiers(SHIFT_DOWN_MASK)
						.condition(WHEN_FOCUSED)
						.action(backwardAction());
	}

	private static <T extends JComponent> KeyEvents.Builder forwardBuilder(T component) {
		return KeyEvents.builder(VK_ENTER)
						.modifiers(component instanceof JTextArea ? CTRL_DOWN_MASK : 0)
						.condition(WHEN_FOCUSED)
						.action(forwardAction());
	}

	/**
	 * An action which transfers focus either forward or backward for a given component
	 */
	private static final class TransferFocusAction extends AbstractAction {

		private final boolean backward;

		/**
		 * @param backward if true the focus is transferred backward
		 */
		private TransferFocusAction(boolean backward) {
			super(backward ? "TransferFocusOnEnter.transferFocusBackward" : "KeyTransferFocusOnEnter.transferFocusForward");
			this.backward = backward;
		}

		/**
		 * Transfers focus according the value of {@code backward}
		 * @param e the action event
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			JComponent source = (JComponent) e.getSource();
			if (backward) {
				source.transferFocusBackward();
			}
			else {
				source.transferFocus();
			}
		}
	}
}
