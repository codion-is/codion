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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.key;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.awt.event.ActionEvent;

import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Objects.requireNonNull;

/**
 * A utility enum for enabling component focus traversal based on the Enter key.
 */
public enum TransferFocusOnEnter {

	/**
	 * <p>Transfer the focus forward when Enter is pressed.
	 * <p>Note that in case of {@link JTextArea} the {@link KeyEvents#MENU_SHORTCUT_MASK} modifier is added.
	 */
	FORWARD {
		@Override
		public void enable(JComponent... components) {
			for (JComponent component : requireNonNull(components)) {
				forward(component).enable(component);
			}
		}
	},
	/**
	 * <p>Transfer the focus backward when Enter is pressed with the
	 * {@link java.awt.event.InputEvent#SHIFT_DOWN_MASK} modifier enabled.
	 */
	BACKWARD {
		@Override
		public void enable(JComponent... components) {
			for (JComponent component : requireNonNull(components)) {
				backward().enable(component);
			}
		}
	},
	/**
	 * <p>Transfer the focus forward when Enter is pressed and backward when Enter
	 * is pressed with the {@link java.awt.event.InputEvent#SHIFT_DOWN_MASK} modifier enabled.
	 * <p>Note that in case of {@link JTextArea} the {@link KeyEvents#MENU_SHORTCUT_MASK}
	 * modifier is added for the forward trigger.
	 */
	FORWARD_BACKWARD {
		@Override
		public void enable(JComponent... components) {
			FORWARD.enable(components);
			BACKWARD.enable(components);
		}
	};

	/**
	 * @param components the components for which to enable focus transfer on enter
	 */
	public abstract void enable(JComponent... components);

	private static final Action TRANSFER_FOCUS_FORWARD = new TransferFocusAction(false);
	private static final Action TRANSFER_FOCUS_BACKWARD = new TransferFocusAction(true);

	private static KeyEvents.Builder backward() {
		return KeyEvents.builder()
						.keyCode(VK_ENTER)
						.modifiers(SHIFT_DOWN_MASK)
						.action(TRANSFER_FOCUS_BACKWARD);
	}

	private static <T extends JComponent> KeyEvents.Builder forward(T component) {
		return KeyEvents.builder()
						.keyCode(VK_ENTER)
						.modifiers(component instanceof JTextArea ? MENU_SHORTCUT_MASK : 0)
						.action(TRANSFER_FOCUS_FORWARD);
	}

	private static final class TransferFocusAction extends AbstractAction {

		private final boolean backward;

		private TransferFocusAction(boolean backward) {
			this.backward = backward;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JComponent component = (JComponent) e.getSource();
			if (backward) {
				component.transferFocusBackward();
			}
			else {
				component.transferFocus();
			}
		}
	}
}
