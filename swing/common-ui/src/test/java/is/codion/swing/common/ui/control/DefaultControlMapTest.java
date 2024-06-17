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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import org.junit.jupiter.api.Test;

import static is.codion.swing.common.ui.control.ControlKeyStrokes.keyStroke;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.awt.event.KeyEvent.VK_S;

public final class DefaultControlMapTest {

	public static final class ControlKeys {
		public static final ControlKey<CommandControl> COMMAND_CONTROL = CommandControl.key(keyStroke(VK_INSERT));
		public static final ControlKey<Controls> CONTROLS = Controls.key();
		public static final ControlKey<ToggleControl> TOGGLE_CONTROL = ToggleControl.key(keyStroke(VK_S, CTRL_DOWN_MASK | ALT_DOWN_MASK));
	}

	@Test
	void test() {
		ControlMap controlMap = new DefaultControlMap(ControlKeys.class);
		controlMap.control(ControlKeys.COMMAND_CONTROL).get();
		controlMap.control(ControlKeys.CONTROLS).get();
		controlMap.control(ControlKeys.TOGGLE_CONTROL).get();
	}
}
