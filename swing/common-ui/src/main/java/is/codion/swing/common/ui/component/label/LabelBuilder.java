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
package is.codion.swing.common.ui.component.label;

import is.codion.common.observer.Observable;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Component;

import static is.codion.common.Configuration.integerValue;

/**
 * A builder for JLabel.
 * @param <T> the type to display in the label (using value.toString() or "" for null).
 */
public interface LabelBuilder<T> extends ComponentBuilder<T, JLabel, LabelBuilder<T>> {

	/**
	 * Specifies the default horizontal alignment used in labels
	 * <ul>
	 * <li>Value type: Integer (SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.CENTER)
	 * <li>Default value: {@link SwingConstants#LEADING}
	 * </ul>
	 */
	PropertyValue<Integer> HORIZONTAL_ALIGNMENT =
					integerValue(LabelBuilder.class.getName() + ".horizontalAlignment", SwingConstants.LEADING);

	/**
	 * @param text the label text
	 * @return this builder instance
	 * @see JLabel#setText(String)
	 */
	LabelBuilder<T> text(@Nullable String text);

	/**
	 * @param text the label text
	 * @return this builder instance
	 * @see JLabel#setText(String)
	 */
	LabelBuilder<T> text(Observable<String> text);

	/**
	 * @param horizontalAlignment the horizontal text alignment
	 * @return this builder instance
	 * @see JLabel#setHorizontalAlignment(int)
	 */
	LabelBuilder<T> horizontalAlignment(int horizontalAlignment);

	/**
	 * @param displayedMnemonic the label mnemonic key code
	 * @return this builder instance
	 * @see JLabel#setDisplayedMnemonic(int)
	 */
	LabelBuilder<T> displayedMnemonic(int displayedMnemonic);

	/**
	 * Overrides {@link #displayedMnemonic(int)}.
	 * @param displayedMnemonic the label mnemonic character
	 * @return this builder instance
	 * @see JLabel#setDisplayedMnemonic(char)
	 */
	LabelBuilder<T> displayedMnemonic(char displayedMnemonic);

	/**
	 * @param component the component to associate with this label
	 * @return this builder instance
	 * @see JLabel#setLabelFor(Component)
	 */
	LabelBuilder<T> labelFor(@Nullable JComponent component);

	/**
	 * @param icon the label icon
	 * @return this builder instance
	 * @see JLabel#setIcon(Icon)
	 */
	LabelBuilder<T> icon(@Nullable Icon icon);

	/**
	 * @param iconTextGap the icon text gap
	 * @return this builder instance
	 * @see JLabel#setIconTextGap(int)
	 */
	LabelBuilder<T> iconTextGap(int iconTextGap);

	/**
	 * @return a new builder
	 */
	static <T> LabelBuilder<T> builder() {
		return new DefaultLabelBuilder<>();
	}
}
