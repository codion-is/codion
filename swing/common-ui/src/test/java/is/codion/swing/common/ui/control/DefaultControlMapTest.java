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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import org.junit.jupiter.api.Test;

import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.awt.event.KeyEvent.VK_S;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultControlMapTest {

	public static final class ControlKeys {
		public static final ControlKey<CommandControl> COMMAND_CONTROL = CommandControl.key("commandControl", keyStroke(VK_INSERT));
		public static final ControlKey<Controls> CONTROLS = Controls.key("controls");
		public static final ControlKey<ToggleControl> TOGGLE_CONTROL = ToggleControl.key("toggleControl", keyStroke(VK_S, CTRL_DOWN_MASK | ALT_DOWN_MASK));
	}

	@Test
	void test() {
		ControlMap controlMap = new DefaultControlMap(ControlKeys.class);
		controlMap.control(ControlKeys.COMMAND_CONTROL).set(command(this::test));
		assertNull(controlMap.control(ControlKeys.CONTROLS).get());
		assertNull(controlMap.control(ControlKeys.TOGGLE_CONTROL).get());
		ControlKey<CommandControl> test = CommandControl.key("test");
		assertThrows(IllegalArgumentException.class, () -> controlMap.control(test));
		assertThrows(IllegalArgumentException.class, () -> controlMap.keyStroke(test));
		assertTrue(controlMap.keyEvent(ControlKeys.COMMAND_CONTROL).isPresent());
		assertFalse(controlMap.keyEvent(ControlKeys.CONTROLS).isPresent());
		ControlMap copy = controlMap.copy();
		assertEquals(copy.controls().size(), controlMap.controls().size());
		assertSame(copy.control(ControlKeys.COMMAND_CONTROL).get(), controlMap.control(ControlKeys.COMMAND_CONTROL).get());
		assertSame(copy.keyStroke(ControlKeys.COMMAND_CONTROL).get(), controlMap.keyStroke(ControlKeys.COMMAND_CONTROL).get());
	}
}
