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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.control.Control;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.event.ActionEvent;

import static is.codion.swing.common.ui.component.Components.button;
import static is.codion.swing.common.ui.component.Components.toggleButton;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ButtonBuilderTest {

	@Test
	void colors() {
		// a control without colors leaves the look and feel colors in place
		JButton button = button()
						.control(Control.command(() -> {}))
						.build();
		assertEquals(UIManager.getColor("Button.background"), button.getBackground());
		assertEquals(UIManager.getColor("Button.foreground"), button.getForeground());
		JToggleButton toggleButton = toggleButton()
						.toggle(Control.toggle(State.state()))
						.build();
		assertEquals(UIManager.getColor("ToggleButton.background"), toggleButton.getBackground());
		assertEquals(UIManager.getColor("ToggleButton.foreground"), toggleButton.getForeground());
		// a control with colors sets them
		button = button()
						.control(Control.builder()
										.command(() -> {})
										.background(Color.RED)
										.foreground(Color.BLUE))
						.build();
		assertEquals(Color.RED, button.getBackground());
		assertEquals(Color.BLUE, button.getForeground());
		// an action color reset to null restores the look and feel color
		Action action = new AbstractAction("action") {
			@Override
			public void actionPerformed(ActionEvent event) {}
		};
		action.putValue("Background", Color.RED);
		button = button()
						.action(action)
						.build();
		assertEquals(Color.RED, button.getBackground());
		action.putValue("Background", null);
		assertEquals(UIManager.getColor("Button.background"), button.getBackground());
	}
}
