/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
