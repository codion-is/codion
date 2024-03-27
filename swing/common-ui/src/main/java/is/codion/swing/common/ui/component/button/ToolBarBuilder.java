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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Controls;

import javax.swing.JToolBar;

import static java.util.Objects.requireNonNull;

/**
 * A builder for a {@link JToolBar}.
 */
public interface ToolBarBuilder extends ControlPanelBuilder<JToolBar, ToolBarBuilder> {

	/**
	 * @param floatable true if the toolbar should be floatable
	 * @return this builder instance
	 * @see JToolBar#setFloatable(boolean)
	 */
	ToolBarBuilder floatable(boolean floatable);

	/**
	 * @param rollover true if rollover should be enabled
	 * @return this builder instance
	 * @see JToolBar#setRollover(boolean)
	 */
	ToolBarBuilder rollover(boolean rollover);

	/**
	 * @param borderPainted true if the border should be painted
	 * @return this builder instance
	 * @see JToolBar#setBorderPainted(boolean)
	 */
	ToolBarBuilder borderPainted(boolean borderPainted);

	/**
	 * @return a new {@link ToolBarBuilder}
	 */
	static ToolBarBuilder builder() {
		return new DefaultToolBarBuilder(null);
	}

	/**
	 * @param controls the controls
	 * @return a new {@link ToolBarBuilder}
	 */
	static ToolBarBuilder builder(Controls controls) {
		return new DefaultToolBarBuilder(requireNonNull(controls));
	}

	/**
	 * @param controlsBuilder the controls builder
	 * @return a new {@link ToolBarBuilder}
	 */
	static ToolBarBuilder builder(Controls.Builder controlsBuilder) {
		return new DefaultToolBarBuilder(requireNonNull(controlsBuilder).build());
	}
}
