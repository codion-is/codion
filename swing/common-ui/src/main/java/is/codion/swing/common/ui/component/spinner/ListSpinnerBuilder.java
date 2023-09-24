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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;

import javax.swing.SpinnerListModel;

import static java.util.Objects.requireNonNull;

/**
 * A builder for JSpinner based on a list of values.
 */
public interface ListSpinnerBuilder<T> extends SpinnerBuilder<T, ListSpinnerBuilder<T>> {

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a builder for a JSpinner
   */
  static <T> ListSpinnerBuilder<T> builder(SpinnerListModel spinnerModel) {
    return new DefaultListSpinnerBuilder<>(spinnerModel, null);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a JSpinner
   */
  static <T> ListSpinnerBuilder<T> builder(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    return new DefaultListSpinnerBuilder<>(spinnerModel, requireNonNull(linkedValue));
  }
}
