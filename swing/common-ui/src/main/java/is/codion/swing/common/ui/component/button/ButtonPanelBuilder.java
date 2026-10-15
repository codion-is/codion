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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.utilities.property.PropertyValue;

import org.jspecify.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import static is.codion.common.utilities.Configuration.enumValue;

/**
 * Builds a JPanel with buttons, in a row or a column.
 * <p>The buttons are stretched across the panel, and share any extra space along it.
 */
public interface ButtonPanelBuilder extends ControlPanelBuilder<JPanel, ButtonPanelBuilder> {

	/**
	 * Specifies how separators are presented in button panels by default.
	 * <ul>
	 * <li>Value type: {@link Separator}
	 * <li>Default value: {@link Separator#LINE}
	 * </ul>
	 * @see #separators(Separator)
	 */
	PropertyValue<Separator> SEPARATORS = enumValue(ButtonPanelBuilder.class.getName() + ".separators", Separator.class, Separator.LINE);

	/**
	 * Default is {@link is.codion.swing.common.ui.layout.Layouts#GAP}.
	 * @param buttonGap the gap between buttons in pixels
	 * @return this builder instance
	 */
	ButtonPanelBuilder buttonGap(int buttonGap);

	/**
	 * Specifies whether the buttons are all sized like the largest one, or each by its own preferred size.
	 * Separators and nested controls keep their own size either way. Default true.
	 * @param fixedButtonSize true if the buttons should all be sized like the largest one
	 * @return this builder instance
	 */
	ButtonPanelBuilder fixedButtonSize(boolean fixedButtonSize);

	/**
	 * Specifies how separators are presented, the space on each side of them being
	 * {@link is.codion.swing.common.ui.layout.Layouts#GAP}, in addition to the button gap.
	 * Default {@link #SEPARATORS}.
	 * @param separators the separator presentation
	 * @return this builder instance
	 */
	ButtonPanelBuilder separators(Separator separators);

	/**
	 * Note that components added via {@link #add(javax.swing.JComponent)} are not added to the button group.
	 * @param buttonGroup the button group to add the buttons based on the controls to
	 * @return this builder instance
	 */
	ButtonPanelBuilder buttonGroup(@Nullable ButtonGroup buttonGroup);

	/**
	 * @return a new button panel builder
	 */
	static ButtonPanelBuilder builder() {
		return new DefaultButtonPanelBuilder();
	}

	/**
	 * Specifies how separators are presented in a button panel.
	 */
	enum Separator {

		/**
		 * A line across the panel, with space on each side
		 */
		LINE,

		/**
		 * Empty space
		 */
		SPACE
	}
}
