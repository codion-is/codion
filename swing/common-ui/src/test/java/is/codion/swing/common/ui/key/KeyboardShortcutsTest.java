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

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.stream.Stream;

import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static org.junit.jupiter.api.Assertions.*;

public final class KeyboardShortcutsTest {

	enum Shortcut implements KeyboardShortcuts.Shortcut {
		ONE(keyStroke(KeyEvent.VK_1)),
		TWO(keyStroke(KeyEvent.VK_2));

		private final KeyStroke defaultKeystroke;

		Shortcut(KeyStroke defaultKeystroke) {
			this.defaultKeystroke = defaultKeystroke;
		}

		@Override
		public Optional<KeyStroke> defaultKeystroke() {
			return Optional.ofNullable(defaultKeystroke);
		}
	}

	@Test
	void test() {
		KeyboardShortcuts<Shortcut> shortcuts = keyboardShortcuts(Shortcut.class);

		assertEquals(KeyEvent.VK_1, shortcuts.keyStroke(Shortcut.ONE).get().getKeyCode());
		assertEquals(KeyEvent.VK_2, shortcuts.keyStroke(Shortcut.TWO).get().getKeyCode());

		KeyboardShortcuts<Shortcut> copy = shortcuts.copy();
		Stream.of(Shortcut.values()).forEach(shortcut -> {
			Value<KeyStroke> keyStrokeValue = shortcuts.keyStroke(shortcut);
			Value<KeyStroke> keyStrokeValueCopy = copy.keyStroke(shortcut);
			assertNotSame(keyStrokeValue, keyStrokeValueCopy);
			assertTrue(keyStrokeValue.isEqualTo(keyStrokeValueCopy.get()));
		});
	}
}
