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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import is.codion.common.value.Value;

import javax.swing.KeyStroke;

import static javax.swing.KeyStroke.getKeyStroke;

/**
 * Specifies a keystroke used for a action shortcut in a UI.
 */
public interface KeyboardShortcut {

  /**
   * @return the Value controlling the keystroke for this shortcut
   */
  Value<KeyStroke> keyStroke();

  /**
   * Creates a {@link Value} instance containing a {@link KeyStroke} with the given keyCode and no modifiers.
   * The resulting value is not nullable and setting it to null will revert back to this default keystroke.
   * @param keyCode the key code
   * @return a keystroke value
   */
  static Value<KeyStroke> keyStrokeValue(int keyCode) {
    return keyStrokeValue(keyCode, 0);
  }

  /**
   * Creates a {@link Value} instance containing a {@link KeyStroke} with the given keyCode and modifiers.
   * The resulting value is not nullable and setting it to null will revert back to this default keystroke.
   * @param keyCode the key code
   * @param modifiers the modifiers
   * @return a keystroke value
   */
  static Value<KeyStroke> keyStrokeValue(int keyCode, int modifiers) {
    KeyStroke keyStroke = getKeyStroke(keyCode, modifiers);

    return Value.value(keyStroke, keyStroke);
  }
}
