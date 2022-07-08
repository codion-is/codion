/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import java.util.Objects;

final class SpinnerItemValue<T> extends AbstractComponentValue<T, JSpinner> {

  SpinnerItemValue(JSpinner spinner) {
    super(spinner);
    if (!(spinner.getModel() instanceof SpinnerListModel)) {
      throw new IllegalArgumentException("Spinner model must be a SpinnerListModel");
    }
    spinner.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected T getComponentValue() {
    Item<T> selectedValue = (Item<T>) getComponent().getModel().getValue();

    return selectedValue == null ? null : selectedValue.getValue();
  }

  @Override
  protected void setComponentValue(T value) {
    SpinnerListModel model = (SpinnerListModel) getComponent().getModel();
    model.getList().stream()
            .map(Item.class::cast)
            .filter(item -> Objects.equals(item.getValue(), value))
            .findFirst()
            .ifPresent(model::setValue);
  }
}
