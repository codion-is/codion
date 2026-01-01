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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Creates and lays out an input panel
 */
public interface InputPanelLayout {

	/**
	 * @param label the label component
	 * @param component the input component
	 * @return a layed out input panel
	 */
	JPanel layout(JComponent label, JComponent component);

	/**
	 * Builds a {@link InputPanelLayout}
	 */
	interface Builder {

		/**
		 * @return a new {@link InputPanelLayout}
		 */
		InputPanelLayout build();
	}

	/**
	 * @return a {@link InputPanelBorderLayoutBuilder}
	 */
	static InputPanelBorderLayoutBuilder border() {
		return new DefaultInputPanelBorderLayout.DefaultBuilder();
	}
}
