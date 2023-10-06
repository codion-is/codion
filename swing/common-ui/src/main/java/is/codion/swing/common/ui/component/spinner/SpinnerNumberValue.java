/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JSpinner;

final class SpinnerNumberValue<T extends Number> extends AbstractComponentValue<T, JSpinner> {

  SpinnerNumberValue(JSpinner spinner) {
    super(spinner);
    spinner.getModel().addChangeListener(e -> notifyListeners());
  }

  @Override
  protected T getComponentValue() {
    return (T) component().getValue();
  }

  @Override
  protected void setComponentValue(T value) {
    component().setValue(value == null ? 0 : value);
  }
}
