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

import is.codion.common.value.Value;

import javax.swing.JRadioButton;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JRadioButton.
 */
public interface RadioButtonBuilder extends ToggleButtonBuilder<JRadioButton, RadioButtonBuilder> {

	/**
	 * @return a builder for a component
	 */
	static RadioButtonBuilder builder() {
		return new DefaultRadioButtonBuilder(null);
	}

	/**
	 * @param linkedValue the value to link to the radion button
	 * @return a builder for a component
	 */
	static RadioButtonBuilder builder(Value<Boolean> linkedValue) {
		return new DefaultRadioButtonBuilder(requireNonNull(linkedValue));
	}
}
