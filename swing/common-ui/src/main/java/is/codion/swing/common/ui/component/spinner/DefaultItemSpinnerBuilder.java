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
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

final class DefaultItemSpinnerBuilder<T> extends AbstractSpinnerBuilder<T, ItemSpinnerBuilder<T>>
        implements ItemSpinnerBuilder<T> {

  DefaultItemSpinnerBuilder(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    super(spinnerModel, linkedValue);
  }

  @Override
  protected ComponentValue<T, JSpinner> createComponentValue(JSpinner component) {
    return new SpinnerItemValue<>(component);
  }
}
