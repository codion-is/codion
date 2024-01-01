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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

final class DefaultKeyboardShortcuts<T extends Enum<T>> implements KeyboardShortcuts<T> {

  private final Class<T> shortcutsClass;
  private final Map<T, Value<KeyStroke>> keyStrokes;

  DefaultKeyboardShortcuts(Class<T> shortcutsClass, Function<T, KeyStroke> defaultKeystrokes) {
    this(requireNonNull(shortcutsClass), Stream.of(shortcutsClass.getEnumConstants())
            .collect(toMap(Function.identity(), shortcutKey -> keyStrokeValue(defaultKeystrokes, shortcutKey))));
  }

  private DefaultKeyboardShortcuts(Class<T> shortcutsClass, Map<T, Value<KeyStroke>> keyStrokes) {
    this.shortcutsClass = shortcutsClass;
    this.keyStrokes = keyStrokes;
  }

  /**
   * @param keyboardShortcut the shortcut key
   * @return the Value controlling the key stroke for the given shortcut key
   */
  public Value<KeyStroke> keyStroke(T keyboardShortcut) {
    return keyStrokes.get(requireNonNull(keyboardShortcut));
  }

  @Override
  public KeyboardShortcuts<T> copy() {
    return new DefaultKeyboardShortcuts<>(shortcutsClass, keyStrokes.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, entry ->
                    Value.value(entry.getValue().get(), entry.getValue().get()))));
  }

  private static <T extends Enum<T>> Value<KeyStroke> keyStrokeValue(Function<T, KeyStroke> defaultKeystrokes, T shortcutKey) {
    KeyStroke keyStroke = requireNonNull(defaultKeystrokes).apply(shortcutKey);
    if (keyStroke == null) {
      throw new IllegalStateException("No default keystroke provided for shortcut key: " + shortcutKey);
    }

    return Value.value(keyStroke, keyStroke);
  }
}
