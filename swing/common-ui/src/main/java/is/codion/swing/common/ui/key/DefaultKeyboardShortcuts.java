/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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

  @Override
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
      throw new IllegalArgumentException("No default keystroke provided for shortcut key: " + shortcutKey);
    }

    return Value.value(keyStroke, keyStroke);
  }
}
