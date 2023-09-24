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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.label;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Component;

/**
 * A builder for JLabel.
 * @param <T> the type to display in the label (using value.toString() or "" for null).
 */
public interface LabelBuilder<T> extends ComponentBuilder<T, JLabel, LabelBuilder<T>> {

  /**
   * Specifies the default horizontal alignment used in labels<br>
   * Value type: Integer (SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.CENTER)<br>
   * Default value: {@link SwingConstants#LEADING}
   */
  PropertyValue<Integer> HORIZONTAL_ALIGNMENT =
          Configuration.integerValue("is.codion.swing.common.ui.component.LabelBuilder.horizontalAlignment", SwingConstants.LEADING);

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
  LabelBuilder<T> labelFor(JComponent component);

  /**
   * @param icon the label icon
   * @return this builder instance
   * @see JLabel#setIcon(Icon)
   */
  LabelBuilder<T> icon(Icon icon);

  /**
   * @param iconTextGap the icon text gap
   * @return this builder instance
   * @see JLabel#setIconTextGap(int)
   */
  LabelBuilder<T> iconTextGap(int iconTextGap);

  /**
   * @param <T> the type to display in the label (using value.toString() or "" for null).
   * @param icon the icon
   * @return a new builder
   */
  static <T> LabelBuilder<T> builder(Icon icon) {
    return new DefaultLabelBuilder<>(icon);
  }

  /**
   * @param <T> the type to display in the label (using value.toString() or "" for null).
   * @param text the label text
   * @return a new builder
   */
  static <T> LabelBuilder<T> builder(String text) {
    return new DefaultLabelBuilder<>(text);
  }

  /**
   * @param <T> the type to display in the label (using value.toString() or "" for null).
   * @param linkedValueObserver the value observer to link to the label text
   * @return a new builder
   */
  static <T> LabelBuilder<T> builder(ValueObserver<T> linkedValueObserver) {
    return new DefaultLabelBuilder<>(linkedValueObserver);
  }
}
