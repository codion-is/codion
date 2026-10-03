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
import is.codion.swing.common.ui.component.button.ControlPanelBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JTextField;
import javax.swing.JToolBar;

/**
 * The {@link ControlPanelBuilder} javadoc snippets, each the region of the same name.
 */
final class ControlPanelBuilderSnippets {

	void toolBar(Controls navigationControls, JTextField searchField, Control settingsControl) {
		JToolBar toolBar = Components.toolBar() // @start region=toolBar
						.controls(navigationControls)
						.separator()
						.add(searchField)
						// the settings button aligned to the right
						.glue()
						.control(settingsControl)
						.floatable(false)
						.build(); // @end
	}
}
