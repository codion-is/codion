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
package is.codion.swing.common.ui.key;

import is.codion.common.value.Value;

import javax.swing.KeyStroke;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

final class DefaultKeyboardShortcuts<T extends Enum<T>> implements KeyboardShortcuts<T> {

  private final Map<T, Value<KeyStroke>> keystrokes = new HashMap<>();

  DefaultKeyboardShortcuts(Class<T> shortcutsClass, Function<T, KeyStroke> defaultKeystrokes) {
    requireNonNull(defaultKeystrokes);
    Stream.of(requireNonNull(shortcutsClass).getEnumConstants()).forEach(shortcutKey ->
            keystrokes.put(shortcutKey, keyStrokeValue(defaultKeystrokes.apply(shortcutKey))));
  }

  /**
   * @param keyboardShortcut the shortcut key
   * @return the Value controlling the key stroke for the given shortcut key
   */
  public Value<KeyStroke> keyStroke(T keyboardShortcut) {
    return keystrokes.get(keyboardShortcut);
  }

  private static Value<KeyStroke> keyStrokeValue(KeyStroke keyStroke) {
    return Value.value(keyStroke, keyStroke);
  }
}
