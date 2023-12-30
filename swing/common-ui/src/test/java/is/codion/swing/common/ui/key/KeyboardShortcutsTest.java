/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.key;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
  }
}
