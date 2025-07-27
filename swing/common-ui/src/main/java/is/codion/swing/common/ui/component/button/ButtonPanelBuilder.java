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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import org.jspecify.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;

/**
 * Builds a JPanel with buttons.
 */
public interface ButtonPanelBuilder extends ControlPanelBuilder<JPanel, ButtonPanelBuilder> {

	/**
	 * Default is {@link is.codion.swing.common.ui.layout.Layouts#GAP}.
	 * @param buttonGap the gap between buttons in pixels
	 * @return this builder instance
	 */
	ButtonPanelBuilder buttonGap(int buttonGap);

	/**
	 * Specifies whether the button grid sizing should be fixed according
	 * to the largest button or flexible. Default false.
	 * @param fixedButtonSize true if the button grid sizing should be fixed
	 * @return this builder instance
	 */
	ButtonPanelBuilder fixedButtonSize(boolean fixedButtonSize);

	/**
	 * @param buttonGroup the button group to add all buttons to
	 * @return this builder instance
	 */
	ButtonPanelBuilder buttonGroup(@Nullable ButtonGroup buttonGroup);

	/**
	 * @return a new button panel builder
	 */
	static ButtonPanelBuilder builder() {
		return new DefaultButtonPanelBuilder();
	}
}
