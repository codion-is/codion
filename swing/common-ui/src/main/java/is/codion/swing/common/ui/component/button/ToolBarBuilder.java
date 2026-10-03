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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import javax.swing.JToolBar;

/**
 * A builder for a {@link JToolBar}.
 */
public interface ToolBarBuilder extends ControlPanelBuilder<JToolBar, ToolBarBuilder> {

	/**
	 * The look and feel default is used if not specified.
	 * @param floatable true if the toolbar should be floatable
	 * @return this builder instance
	 * @see JToolBar#setFloatable(boolean)
	 */
	ToolBarBuilder floatable(boolean floatable);

	/**
	 * The look and feel default, {@code ToolBar.isRollover}, is used if not specified.
	 * @param rollover true if rollover should be enabled
	 * @return this builder instance
	 * @see JToolBar#setRollover(boolean)
	 */
	ToolBarBuilder rollover(boolean rollover);

	/**
	 * The look and feel default is used if not specified.
	 * @param borderPainted true if the border should be painted
	 * @return this builder instance
	 * @see JToolBar#setBorderPainted(boolean)
	 */
	ToolBarBuilder borderPainted(boolean borderPainted);

	/**
	 * Adds glue, horizontal or vertical depending on the toolbar orientation,
	 * for example to align the subsequent items to the right of a horizontal toolbar.
	 * @return this builder instance
	 * @see javax.swing.Box#createHorizontalGlue()
	 * @see javax.swing.Box#createVerticalGlue()
	 */
	ToolBarBuilder glue();

	/**
	 * @return a new {@link ToolBarBuilder}
	 */
	static ToolBarBuilder builder() {
		return new DefaultToolBarBuilder();
	}
}
