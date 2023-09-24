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
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JSpinner;

/**
 * A builder for JSpinner.
 */
public interface SpinnerBuilder<T, B extends SpinnerBuilder<T, B>> extends ComponentBuilder<T, JSpinner, B> {

  /**
   * @param editable false if the spinner field should not be editable
   * @return this builder instance
   */
  B editable(boolean editable);

  /**
   * @param columns the number of colums in the spinner text component
   * @return this builder instance
   */
  B columns(int columns);

  /**
   * Enable mouse wheel scrolling on the spinner
   * @param mouseWheelScrolling true if mouse wheel scrolling should be enabled
   * @return this builder instance
   */
  B mouseWheelScrolling(boolean mouseWheelScrolling);

  /**
   * Enable reversed mouse wheel scrolling on the spinner
   * @param mouseWheelScrollingReversed if true then up/away decreases the value and down/towards increases it.
   * @return this builder instance
   */
  B mouseWheelScrollingReversed(boolean mouseWheelScrollingReversed);

  /**
   * @param horizontalAlignment the horizontal text alignment
   * @return this builder instance
   */
  B horizontalAlignment(int horizontalAlignment);
}
