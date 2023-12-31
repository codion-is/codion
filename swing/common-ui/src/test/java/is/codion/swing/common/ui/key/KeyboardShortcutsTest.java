/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.key;

import is.codion.common.value.Value;

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.stream.Stream;

import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static org.junit.jupiter.api.Assertions.*;

public final class KeyboardShortcutsTest {

  enum Shortcut {
    ONE, TWO
  }

  @Test
  void test() {
    assertThrows(IllegalStateException.class, () -> keyboardShortcuts(Shortcut.class, shortcut -> {
      if (shortcut == Shortcut.ONE) {
        return keyStroke(KeyEvent.VK_1);
      }

      return null;
    }));

    KeyboardShortcuts<Shortcut> shortcuts = keyboardShortcuts(Shortcut.class, shortcut -> {
      switch (shortcut) {
        case ONE:
          return keyStroke(KeyEvent.VK_1);
        case TWO:
          return keyStroke(KeyEvent.VK_2);
        default:
          throw new IllegalArgumentException();
      }
    });

    assertEquals(KeyEvent.VK_1, shortcuts.keyStroke(Shortcut.ONE).get().getKeyCode());
    assertEquals(KeyEvent.VK_2, shortcuts.keyStroke(Shortcut.TWO).get().getKeyCode());

    KeyboardShortcuts<Shortcut> copy = shortcuts.copy();
    Stream.of(Shortcut.values()).forEach(shortcut -> {
      Value<KeyStroke> keyStrokeValue = shortcuts.keyStroke(shortcut);
      Value<KeyStroke> keyStrokeValueCopy = copy.keyStroke(shortcut);
      assertNotSame(keyStrokeValue, keyStrokeValueCopy);
      assertTrue(keyStrokeValue.equalTo(keyStrokeValueCopy.get()));
    });
  }
}
