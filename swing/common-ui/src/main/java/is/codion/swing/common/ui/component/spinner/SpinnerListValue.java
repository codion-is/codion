/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import java.util.List;

final class SpinnerListValue<T> extends AbstractComponentValue<T, JSpinner> {

  SpinnerListValue(JSpinner spinner) {
    super(spinner);
    if (!(spinner.getModel() instanceof SpinnerListModel)) {
      throw new IllegalArgumentException("Spinner model must be a SpinnerListModel");
    }
    spinner.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected T getComponentValue() {
    return (T) component().getValue();
  }

  @Override
  protected void setComponentValue(T value) {
    List<?> list = ((SpinnerListModel) component().getModel()).getList();
    component().setValue(value == null ? (list.isEmpty() ? null : list.get(0)) : value);
  }
}
