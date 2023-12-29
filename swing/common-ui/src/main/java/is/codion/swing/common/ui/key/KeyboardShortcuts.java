/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.key;

import is.codion.common.value.Value;

import javax.swing.KeyStroke;
import java.util.function.Function;

import static javax.swing.KeyStroke.getKeyStroke;

/**
 * Holds keyboard shortcut keyStrokes.
 * @param <T> the shortcut key
 * @see #keyboardShortcuts(Class, Function)
 */
public interface KeyboardShortcuts<T extends Enum<T>> {

  /**
   * @param keyboardShortcut the shortcut key
   * @return the Value controlling the key stroke for the given shortcut key
   */
  Value<KeyStroke> keyStroke(T keyboardShortcut);

  /**
   * @param shortcutClass the shortcut key class
   * @param defaults provides the default keystroke for each shortcut key
   * @return a new {@link KeyboardShortcuts} instance
   * @param <T> the shortcut key type
   */
  static <T extends Enum<T>> KeyboardShortcuts<T> keyboardShortcuts(Class<T> shortcutClass, Function<T, KeyStroke> defaults) {
    return new DefaultKeyboardShortcuts<>(shortcutClass, defaults);
  }

  /**
   * Creates a {@link KeyStroke} with the given keyCode and no modifiers.
   * @param keyCode the key code
   * @return a keystroke value
   */
  static KeyStroke keyStroke(int keyCode) {
    return keyStroke(keyCode, 0);
  }

  /**
   * Creates a {@link KeyStroke} with the given keyCode and modifiers.
   * @param keyCode the key code
   * @param modifiers the modifiers
   * @return a keystroke value
   */
  static KeyStroke keyStroke(int keyCode, int modifiers) {
    return getKeyStroke(keyCode, modifiers);
  }

  /**
   * Specifies a keystroke used for a action shortcut in a UI.
   */
  interface Shortcut {}
}
