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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;

import java.util.ResourceBundle;

/**
 * A {@link Value.Validator} restricting the maximum length of a string value.
 */
final class StringLengthValidator implements Value.Validator<String> {

	private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(StringLengthValidator.class.getName());

	private int maximumLength;

	/**
	 * @param maximumLength the maximum length, -1 for no limit
	 */
	StringLengthValidator(int maximumLength) {
		this.maximumLength = maximumLength;
	}

	/**
	 * @return the maximum length
	 */
	int getMaximumLength() {
		return maximumLength;
	}

	/**
	 * @param maximumLength the maximum length, -1 for no limit
	 */
	void setMaximumLength(int maximumLength) {
		this.maximumLength = maximumLength < 0 ? -1 : maximumLength;
	}

	@Override
	public void validate(String text) {
		if (text != null && maximumLength >= 0 && text.length() > maximumLength) {
			throw new IllegalArgumentException(MESSAGES.getString("length_exceeds_maximum") + ": " + maximumLength);
		}
	}
}
