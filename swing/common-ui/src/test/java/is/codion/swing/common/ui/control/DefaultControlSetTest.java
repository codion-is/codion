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

import static is.codion.swing.common.ui.control.ControlShortcuts.keyStroke;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.awt.event.KeyEvent.VK_S;

public final class DefaultControlSetTest {

	public interface ControlTypes {
		ControlId<CommandControl> COMMAND_CONTROL = ControlId.commandControl(keyStroke(VK_INSERT));
		ControlId<Controls> CONTROLS = ControlId.controls();
		ControlId<ToggleControl> TOGGLE_CONTROL = ControlId.toggleControl(keyStroke(VK_S, CTRL_DOWN_MASK | ALT_DOWN_MASK));
	}

	@Test
	void test() {
		ControlSet controlSet = new DefaultControlSet(ControlTypes.class);
		controlSet.control(ControlTypes.COMMAND_CONTROL).get();
		controlSet.control(ControlTypes.CONTROLS).get();
		controlSet.control(ControlTypes.TOGGLE_CONTROL).get();
	}
}
