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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.value.Value;

import javax.swing.KeyStroke;

import static java.util.Objects.requireNonNull;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * Manages keyboard shortcuts for Controls.
 */
public interface ControlShortcuts {

	/**
	 * @param controlId the control id
	 * @return the {@link Value} controlling the key stroke for the given control
	 * @throws IllegalArgumentException in case no control is associated with the given control id
	 */
	Value<KeyStroke> keyStroke(ControlId<?> controlId);

	/**
	 * @return a copy of this {@link ControlShortcuts} instance
	 */
	ControlShortcuts copy();

	/**
	 * @param controlIdsClass the class containing the control ids
	 * @return a new {@link ControlShortcuts} instance
	 */
	static ControlShortcuts controlShortcuts(Class<?> controlIdsClass) {
		return new DefaultControlShortcuts(requireNonNull(controlIdsClass));
	}

	/**
	 * Creates a {@link KeyStroke} with the given keyCode and no modifiers.
	 * @param keyCode the key code
	 * @return a keystroke value
	 * @see KeyStroke#getKeyStroke(int, int)
	 */
	static KeyStroke keyStroke(int keyCode) {
		return keyStroke(keyCode, 0);
	}

	/**
	 * Creates a {@link KeyStroke} with the given keyCode and modifiers.
	 * @param keyCode the key code
	 * @param modifiers the modifiers
	 * @return a keystroke value
	 * @see KeyStroke#getKeyStroke(int, int)
	 */
	static KeyStroke keyStroke(int keyCode, int modifiers) {
		return getKeyStroke(keyCode, modifiers);
	}
}
