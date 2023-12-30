/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

  private final Map<T, Value<KeyStroke>> keyStrokes;

  DefaultKeyboardShortcuts(Class<T> shortcutsClass, Function<T, KeyStroke> defaultKeystrokes) {
    requireNonNull(shortcutsClass);
    requireNonNull(defaultKeystrokes);
    keyStrokes = Stream.of(shortcutsClass.getEnumConstants())
            .collect(toMap(Function.identity(), shortcutKey -> keyStrokeValue(defaultKeystrokes, shortcutKey)));
  }

  /**
   * @param keyboardShortcut the shortcut key
   * @return the Value controlling the key stroke for the given shortcut key
   */
  public Value<KeyStroke> keyStroke(T keyboardShortcut) {
    return keyStrokes.get(requireNonNull(keyboardShortcut));
  }

  private Value<KeyStroke> keyStrokeValue(Function<T, KeyStroke> defaultKeystrokes, T shortcutKey) {
    KeyStroke keyStroke = defaultKeystrokes.apply(shortcutKey);
    if (keyStroke == null) {
      throw new IllegalStateException("No default keystroke provided for shortcut key: " + shortcutKey);
    }

    return Value.value(keyStroke, keyStroke);
  }
}
