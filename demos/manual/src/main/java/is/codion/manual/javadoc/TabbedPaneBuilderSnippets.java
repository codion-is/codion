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
package is.codion.manual.javadoc;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.event.KeyEvent;

/**
 * The {@link TabbedPaneBuilder} javadoc snippets, each the region of the same name.
 */
final class TabbedPaneBuilderSnippets {

	void usage(Icon firstTabIcon, Icon secondTabIcon) {
		Components.tabbedPane() // @start region=usage
						.tab("First Tab", new JLabel("First"))
						.tab("Second Tab", new JLabel("Second"))
						.build();

		Components.tabbedPane()
						.tabPlacement(SwingConstants.TOP)
						.tabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
						.tab("First Tab")
						.component(new JLabel("First"))
						.mnemonic(KeyEvent.VK_1)
						.toolTipText("This is the first tab")
						.icon(firstTabIcon)
						.add()
						.tab("Second Tab")
						.component(new JLabel("Second"))
						.mnemonic(KeyEvent.VK_2)
						.toolTipText("This is the second tab")
						.icon(secondTabIcon)
						.add()
						.build(); // @end
	}
}
