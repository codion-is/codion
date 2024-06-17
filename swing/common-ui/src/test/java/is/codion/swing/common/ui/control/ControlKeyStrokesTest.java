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
package is.codion.swing.common.ui.control;

import is.codion.common.value.Value;

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.stream.Stream;

import static is.codion.swing.common.ui.control.ControlKeyStrokes.controlKeyStrokes;
import static is.codion.swing.common.ui.control.ControlKeyStrokes.keyStroke;
import static is.codion.swing.common.ui.control.ControlKeyStrokesTest.ControlKeys.ONE;
import static is.codion.swing.common.ui.control.ControlKeyStrokesTest.ControlKeys.TWO;
import static org.junit.jupiter.api.Assertions.*;

public final class ControlKeyStrokesTest {

	interface ControlKeys {
		ControlKey<CommandControl> ONE = CommandControl.key(keyStroke(KeyEvent.VK_1));
		ControlKey<CommandControl> TWO = CommandControl.key(keyStroke(KeyEvent.VK_2));
	}

	@Test
	void test() {
		ControlKeyStrokes keyStrokes = controlKeyStrokes(ControlKeys.class);

		assertEquals(KeyEvent.VK_1, keyStrokes.keyStroke(ONE).get().getKeyCode());
		assertEquals(KeyEvent.VK_2, keyStrokes.keyStroke(TWO).get().getKeyCode());

		ControlKeyStrokes copy = keyStrokes.copy();
		Stream.of(ONE, TWO).forEach(shortcut -> {
			Value<KeyStroke> keyStrokeValue = keyStrokes.keyStroke(shortcut);
			Value<KeyStroke> keyStrokeValueCopy = copy.keyStroke(shortcut);
			assertNotSame(keyStrokeValue, keyStrokeValueCopy);
			assertTrue(keyStrokeValue.isEqualTo(keyStrokeValueCopy.get()));
		});
	}
}
