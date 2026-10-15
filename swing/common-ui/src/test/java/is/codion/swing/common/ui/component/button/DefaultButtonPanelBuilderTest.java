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

import is.codion.swing.common.ui.component.button.ButtonPanelBuilder.Separator;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.Layouts;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;

import static javax.swing.SwingConstants.VERTICAL;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultButtonPanelBuilderTest {

	private static final int GAP = Layouts.GAP.getOrThrow();

	@Test
	void fixedButtonSize() {
		Component[] components = layout(ButtonPanelBuilder.builder()
						.control(control("A"))
						.control(control("A much longer caption"))
						.separator()
						.control(control("B")));
		assertEquals(4, components.length);
		// the buttons all sized like the largest one
		assertEquals(components[1].getWidth(), components[0].getWidth());
		assertEquals(components[1].getWidth(), components[3].getWidth());
		// the separator, a line by default, keeping its own size, the default gap on each side
		assertFalse(components[2] instanceof AbstractButton);
		assertTrue(components[2].getWidth() >= 2 * GAP);
		assertTrue(components[2].getWidth() < components[0].getWidth());
		// the button gap between each, everything stretched across
		for (int i = 1; i < components.length; i++) {
			assertEquals(components[i - 1].getX() + components[i - 1].getWidth() + GAP, components[i].getX());
			assertEquals(components[0].getHeight(), components[i].getHeight());
		}
	}

	@Test
	void flexibleButtonSize() {
		Component[] components = layout(ButtonPanelBuilder.builder()
						.control(control("A"))
						.control(control("A much longer caption"))
						.fixedButtonSize(false));
		assertEquals(components[0].getPreferredSize().width, components[0].getWidth());
		assertEquals(components[1].getPreferredSize().width, components[1].getWidth());
		assertTrue(components[0].getWidth() < components[1].getWidth());
	}

	@Test
	void space() {
		Component[] components = layout(ButtonPanelBuilder.builder()
						.control(control("A"))
						.separator()
						.control(control("B"))
						.separators(Separator.SPACE));
		assertEquals(2 * GAP, components[1].getWidth());

		components = layout(ButtonPanelBuilder.builder()
						.control(control("A"))
						.separator()
						.control(control("B"))
						.separators(Separator.SPACE)
						.orientation(VERTICAL));
		assertEquals(2 * GAP, components[1].getHeight());
		// stretched across
		assertEquals(components[0].getWidth(), components[1].getWidth());
	}

	@Test
	void nestedControls() {
		JPanel panel = ButtonPanelBuilder.builder()
						.control(control("A"))
						.controls(Controls.builder()
										.control(Controls.builder()
														.control(control("A much longer caption"))
														.control(control("C")))
										.build())
						.control(control("D"))
						.build();
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();
		assertEquals(3, panel.getComponentCount());
		JPanel nested = assertInstanceOf(JPanel.class, panel.getComponent(1));
		nested.doLayout();
		// the outer panel sizing all the buttons, its own and the nested ones, like the largest one, a nested one
		int buttonWidth = nested.getComponent(0).getWidth();
		assertEquals(buttonWidth, nested.getComponent(1).getWidth());
		assertEquals(buttonWidth, panel.getComponent(0).getWidth());
		assertEquals(buttonWidth, panel.getComponent(2).getWidth());
		assertEquals(2 * buttonWidth + GAP, nested.getWidth());

		// the extra space shared by the button, the nested controls getting a share for each of theirs
		panel.setSize(panel.getWidth() + 100, panel.getHeight());
		panel.doLayout();
		nested.doLayout();
		assertEquals(buttonWidth + 25, panel.getComponent(0).getWidth());
		assertEquals(buttonWidth + 25, nested.getComponent(0).getWidth());
		assertEquals(buttonWidth + 25, nested.getComponent(1).getWidth());
		assertEquals(buttonWidth + 25, panel.getComponent(2).getWidth());
	}

	@Test
	void nestedControlsSizedByTheirButtons() {
		JPanel panel = ButtonPanelBuilder.builder()
						.control(control("A much longer caption"))
						.controls(Controls.builder()
										.control(Controls.builder()
														.control(control("B"))
														.control(control("C")))
										.build())
						.build();
		JPanel nested = (JPanel) panel.getComponent(1);
		// the preferred size of the nested panel not used, a panel caching its preferred size until it is invalidated,
		// which a button outside it changing does not do
		nested.setPreferredSize(new Dimension(1, 1));
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();
		nested.doLayout();
		int buttonWidth = panel.getComponent(0).getWidth();
		assertEquals(2 * buttonWidth + GAP, nested.getWidth());
		assertEquals(buttonWidth, nested.getComponent(0).getWidth());
		assertEquals(buttonWidth, nested.getComponent(1).getWidth());
	}

	@Test
	void extraSpace() {
		JPanel panel = ButtonPanelBuilder.builder()
						.control(control("A"))
						.separator()
						.control(control("B"))
						.build();
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();
		int buttonWidth = panel.getComponent(0).getWidth();
		int separatorWidth = panel.getComponent(1).getWidth();
		// shared by the buttons, the separator keeping its size
		panel.setSize(panel.getWidth() + 100, panel.getHeight());
		panel.doLayout();
		assertEquals(buttonWidth + 50, panel.getComponent(0).getWidth());
		assertEquals(separatorWidth, panel.getComponent(1).getWidth());
		assertEquals(buttonWidth + 50, panel.getComponent(2).getWidth());
		assertEquals(panel.getWidth(), panel.getComponent(2).getX() + panel.getComponent(2).getWidth());
	}

	@Test
	void invisibleComponents() {
		JLabel label = new JLabel("label");
		label.setVisible(false);
		Component[] components = layout(ButtonPanelBuilder.builder()
						.control(control("A"))
						.add(label)
						.control(control("B")));
		assertEquals(components[0].getX() + components[0].getWidth() + GAP, components[2].getX());
	}

	private static Component[] layout(ButtonPanelBuilder builder) {
		JPanel panel = builder.build();
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();

		return panel.getComponents();
	}

	private static Control control(String caption) {
		return Control.builder()
						.command(() -> {})
						.caption(caption)
						.build();
	}
}
