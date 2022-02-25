/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import java.util.Objects;

final class SpinnerItemValue<T> extends AbstractComponentValue<T, JSpinner> {

  SpinnerItemValue(final JSpinner spinner) {
    super(spinner);
    if (!(spinner.getModel() instanceof SpinnerListModel)) {
      throw new IllegalArgumentException("Spinner model must be a SpinnerListModel");
    }
    spinner.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected T getComponentValue(final JSpinner component) {
    Item<T> selectedValue = (Item<T>) component.getModel().getValue();

    return selectedValue == null ? null : selectedValue.getValue();
  }

  @Override
  protected void setComponentValue(final JSpinner component, final T value) {
    SpinnerListModel model = (SpinnerListModel) component.getModel();
    model.getList().stream()
            .map(Item.class::cast)
            .filter(item -> Objects.equals(item.getValue(), value))
            .findFirst()
            .ifPresent(model::setValue);
  }
}
