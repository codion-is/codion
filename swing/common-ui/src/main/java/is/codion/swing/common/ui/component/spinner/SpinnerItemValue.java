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

import is.codion.common.item.Item;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import java.util.Objects;

final class SpinnerItemValue<T> extends AbstractComponentValue<T, JSpinner> {

  SpinnerItemValue(JSpinner spinner) {
    super(spinner);
    if (!(spinner.getModel() instanceof SpinnerListModel)) {
      throw new IllegalArgumentException("Spinner model must be a SpinnerListModel");
    }
    spinner.getModel().addChangeListener(e -> notifyListeners());
  }

  @Override
  protected T getComponentValue() {
    Item<T> selectedValue = (Item<T>) component().getModel().getValue();

    return selectedValue == null ? null : selectedValue.get();
  }

  @Override
  protected void setComponentValue(T value) {
    SpinnerListModel model = (SpinnerListModel) component().getModel();
    model.getList().stream()
            .map(Item.class::cast)
            .filter(item -> Objects.equals(item.get(), value))
            .findFirst()
            .ifPresent(model::setValue);
  }
}
