/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

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
    return (T) getComponent().getValue();
  }

  @Override
  protected void setComponentValue(T value) {
    getComponent().setValue(value == null ? ((SpinnerListModel) getComponent().getModel()).getList().get(0) : value);
  }
}
