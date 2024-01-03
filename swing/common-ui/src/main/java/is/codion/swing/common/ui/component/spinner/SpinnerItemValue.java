/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
