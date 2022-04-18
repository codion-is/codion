/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JSpinner;

final class SpinnerNumberValue<T extends Number> extends AbstractComponentValue<T, JSpinner> {

  SpinnerNumberValue(JSpinner spinner) {
    super(spinner);
    spinner.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected T getComponentValue(JSpinner component) {
    return (T) component.getValue();
  }

  @Override
  protected void setComponentValue(JSpinner component, T value) {
    component.setValue(value == null ? 0 : value);
  }
}
