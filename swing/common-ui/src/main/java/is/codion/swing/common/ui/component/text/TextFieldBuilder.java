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
package is.codion.swing.common.ui.component.text;

import is.codion.common.property.PropertyValue;

import javax.swing.Action;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.text.Format;

import static is.codion.common.Configuration.booleanValue;

/**
 * Builds a JTextField.
 * @param <T> the type the text field represents
 * @param <C> the text field type
 * @param <B> the builder type
 */
public interface TextFieldBuilder<T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> extends TextComponentBuilder<T, C, B> {


	/**
	 * Specifies whether text fields should select all on focus gained by default.
	 * <ul>
	 * <li>Value type:Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> SELECT_ALL_ON_FOCUS_GAINED =
					booleanValue(TextFieldBuilder.class.getName() + ".selectAllOnFocusGained", false);

	/**
	 * @param columns the number of colums in the text component
	 * @return this builder instance
	 * @see JTextField#setColumns(int)
	 */
	B columns(int columns);

	/**
	 * Note that this disables {@link #transferFocusOnEnter(boolean)}.
	 * @param action the action to associate with the text field
	 * @return this builder instance
	 * @see JTextField#setAction(Action)
	 */
	B action(Action action);

	/**
	 * Note that this disables {@link #transferFocusOnEnter(boolean)}.
	 * @param actionListener the action listener
	 * @return this builder instance
	 * @see JTextField#addActionListener(ActionListener)
	 */
	B actionListener(ActionListener actionListener);

	/**
	 * Associates the given format with the text field. Note that the format instance is
	 * cloned on build, so format instances can be reused when calling this method.
	 * @param format the format
	 * @return this builder instance
	 */
	B format(Format format);

	/**
	 * @param horizontalAlignment the horizontal text alignment
	 * @return this builder instance
	 * @see JTextField#setHorizontalAlignment(int)
	 */
	B horizontalAlignment(int horizontalAlignment);

	/**
	 * @param hint the hint to display when the field is empty and unfocused
	 * @return this builder instance
	 */
	B hint(String hint);

	/**
	 * @param <T> the value type
	 * @param <C> the text field type
	 * @param <B> the builder type
	 * @param valueClass the value class
	 * @return a builder for a component
	 */
	static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> builder(Class<T> valueClass) {
		return new DefaultTextFieldBuilder<>(valueClass);
	}
}
