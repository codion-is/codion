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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import org.jspecify.annotations.Nullable;

import javax.swing.JTextField;

final class CharacterFieldValue extends AbstractTextComponentValue<Character, JTextField> {

	CharacterFieldValue(JTextField textField, UpdateOn updateOn) {
		super(textField, null, updateOn);
	}

	@Override
	protected @Nullable Character getComponentValue() {
		String string = component().getText();

		return string.isEmpty() ? null : string.charAt(0);
	}

	@Override
	protected void setComponentValue(@Nullable Character value) {
		component().setText(value == null ? "" : String.valueOf(value));
	}
}
