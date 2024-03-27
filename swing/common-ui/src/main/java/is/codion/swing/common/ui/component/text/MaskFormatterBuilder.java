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
package is.codion.swing.common.ui.component.text;

import javax.swing.text.MaskFormatter;
import java.text.ParseException;

/**
 * Builds a {@link MaskFormatter} instance.
 */
public interface MaskFormatterBuilder {

	/**
	 * @param mask the format mask string
	 * @return this builder instance
	 * @see javax.swing.text.MaskFormatter#setMask(String)
	 */
	MaskFormatterBuilder mask(String mask);

	/**
	 * @param valueContainsLiteralCharacters true if the value should contain literal characters
	 * @return this builder instance
	 * @see javax.swing.text.MaskFormatter#setValueContainsLiteralCharacters(boolean)
	 */
	MaskFormatterBuilder valueContainsLiteralCharacters(boolean valueContainsLiteralCharacters);

	/**
	 * @param placeholder the placeholder
	 * @return this builder instance
	 * @see javax.swing.text.MaskFormatter#setPlaceholder(String)
	 */
	MaskFormatterBuilder placeholder(String placeholder);

	/**
	 * @param placeholderCharacter the placeholder character
	 * @return this builder instance
	 * @see javax.swing.text.MaskFormatter#setPlaceholderCharacter(char)
	 */
	MaskFormatterBuilder placeholderCharacter(char placeholderCharacter);

	/**
	 * @param allowsInvalid true if this field should allow invalid values
	 * @return this builder instance
	 * @see javax.swing.text.MaskFormatter#setAllowsInvalid(boolean)
	 */
	MaskFormatterBuilder allowsInvalid(boolean allowsInvalid);

	/**
	 * @param commitsOnValidEdit true if value should be committed on valid edit
	 * @return this builder instance
	 * @see javax.swing.text.MaskFormatter#setCommitsOnValidEdit(boolean)
	 */
	MaskFormatterBuilder commitsOnValidEdit(boolean commitsOnValidEdit);

	/**
	 * @param validCharacters the valid characters
	 * @return this builder instance
	 * @see javax.swing.text.MaskFormatter#setValidCharacters(String)
	 */
	MaskFormatterBuilder validCharacters(String validCharacters);

	/**
	 * @param invalidCharacters the invalid characters
	 * @return this builder instance
	 * @see javax.swing.text.MaskFormatter#setInvalidCharacters(String)
	 */
	MaskFormatterBuilder invalidCharacters(String invalidCharacters);

	/**
	 * @param overwriteMode true if new characters should overwrite existing characters
	 * @return this builder instance
	 * @see javax.swing.text.MaskFormatter#setOverwriteMode(boolean)
	 */
	MaskFormatterBuilder overwriteMode(boolean overwriteMode);

	/**
	 * If set to true then {@link MaskFormatter#stringToValue(String)} returns null when
	 * it encounters an empty string, instead of throwing a {@link ParseException}.
	 * @param emptyStringToNullValue if true then an empty string translates to a null value
	 * @return this builder instance
	 * @see MaskFormatter#stringToValue(String)
	 */
	MaskFormatterBuilder emptyStringToNullValue(boolean emptyStringToNullValue);

	/**
	 * If set to true then {@link MaskFormatter#stringToValue(String)} returns null when
	 * it encounters an unparsable string, instead of throwing a {@link ParseException}.
	 * @param invalidStringToNullValue if true then an unparsable string translates to a null value
	 * @return this builder instance
	 * @see MaskFormatter#stringToValue(String)
	 */
	MaskFormatterBuilder invalidStringToNullValue(boolean invalidStringToNullValue);

	/**
	 * @return a new {@link MaskFormatter} instance based on this builder
	 * @throws ParseException if the mask does not contain valid mask characters
	 */
	MaskFormatter build() throws ParseException;

	/**
	 * @return a new {@link MaskFormatterBuilder} instance
	 */
	static MaskFormatterBuilder builder() {
		return new DefaultMaskFormatter.DefaultMaskFormatterBuilder();
	}
}
