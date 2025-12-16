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
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Controls;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.event.ActionEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultMenuBuilderTest {

	@Test
	void separators() {
		DefaultMenuBuilder builder = new DefaultMenuBuilder(Controls.builder()
						.action(Controls.SEPARATOR)
						.action(Controls.SEPARATOR)
						.build());
		JMenu menu = builder.build();
		assertEquals(0, menu.getItemCount());

		AbstractAction action = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {}
		};
		builder = new DefaultMenuBuilder(Controls.builder()
						.action(action)
						.action(Controls.SEPARATOR)
						.build());
		menu = builder.build();
		assertEquals(1, menu.getItemCount());
		builder = new DefaultMenuBuilder(Controls.builder()
						.action(Controls.SEPARATOR)
						.action(action)
						.action(Controls.SEPARATOR)
						.build());
		menu = builder.build();
		assertEquals(1, menu.getItemCount());
		builder = new DefaultMenuBuilder(Controls.builder()
						.action(Controls.SEPARATOR)
						.action(Controls.SEPARATOR)
						.action(action)
						.action(Controls.SEPARATOR)
						.action(action)
						.action(Controls.SEPARATOR)
						.action(Controls.SEPARATOR)
						.build());
		menu = builder.build();
		assertEquals(3, menu.getItemCount());
	}

	@Test
	void popupMenu() {
		AbstractAction action1 = new AbstractAction("One") {
			@Override
			public void actionPerformed(ActionEvent e) {}
		};
		AbstractAction action2 = new AbstractAction("Two") {
			@Override
			public void actionPerformed(ActionEvent e) {}
		};
		AbstractAction action3 = new AbstractAction("Three") {
			@Override
			public void actionPerformed(ActionEvent e) {}
		};
		DefaultMenuBuilder builder = new DefaultMenuBuilder(Controls.builder()
						.caption("Test")
						.action(action1)
						.action(action2)
						.controls(Controls.builder()
										.caption("Test2")
										.action(action3)
										.build())
						.build());
		JPopupMenu popupMenu = builder.buildPopupMenu();
		Component[] components = popupMenu.getComponents();
		assertEquals(3, components.length);
		JMenuItem item = (JMenuItem) components[0];
		assertEquals("One", item.getText());
		item = (JMenuItem) components[1];
		assertEquals("Two", item.getText());
		JMenu submenu = (JMenu) components[2];
		assertEquals("Test2", submenu.getText());
		assertEquals(1, submenu.getItemCount());
		item = submenu.getItem(0);
		assertEquals("Three", item.getText());
	}
}
