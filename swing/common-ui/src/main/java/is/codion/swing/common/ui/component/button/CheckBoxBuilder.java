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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import javax.swing.JCheckBox;

/**
 * Builds a JCheckBox.
 */
public interface CheckBoxBuilder extends ToggleButtonBuilder<JCheckBox, CheckBoxBuilder> {

	/**
	 * @param nullable if true then a {@link NullableCheckBox} is built.
	 * @return this builder instance
	 */
	CheckBoxBuilder nullable(boolean nullable);

	/**
	 * @return a builder for a component
	 */
	static CheckBoxBuilder builder() {
		return new DefaultCheckBoxBuilder();
	}
}
