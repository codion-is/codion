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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.key;

import is.codion.common.value.Value;

import javax.swing.KeyStroke;

import static javax.swing.KeyStroke.getKeyStroke;

/**
 * Holds mutable keyboard shortcut keyStrokes, mapped to enum based shortcut keys.
 * @param <T> the shortcut key type
 * @see #keyboardShortcuts(Class)
 */
public interface KeyboardShortcuts<T extends Enum<T> & KeyboardShortcuts.Shortcut> {

	/**
	 * @param shortcut the shortcut key
	 * @return the {@link Value} controlling the key stroke for the given shortcut key
	 */
	Value<KeyStroke> keyStroke(T shortcut);

	/**
	 * @return a copy of this {@link KeyboardShortcuts} instance
	 */
	KeyboardShortcuts<T> copy();

	/**
	 * Specifies a keyboard shortcut providing a default key stroke.
	 */
	interface Shortcut {

		/**
		 * @return the default keystroke for this shortcut
		 */
		KeyStroke defaultKeystroke();
	}

	/**
	 * @param shortcutKeyClass the shortcut key class
	 * @param <T> the shortcut key type
	 * @return a new {@link KeyboardShortcuts} instance
	 * @throws IllegalArgumentException in case any of the shortcut keys is missing a default keystroke
	 */
	static <T extends Enum<T> & Shortcut> KeyboardShortcuts<T> keyboardShortcuts(Class<T> shortcutKeyClass) {
		return new DefaultKeyboardShortcuts<>(shortcutKeyClass);
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
