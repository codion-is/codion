/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JSpinner;

final class SpinnerNumberValue<T extends Number> extends AbstractComponentValue<T, JSpinner> {

  SpinnerNumberValue(final JSpinner spinner) {
    super(spinner);
    spinner.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected T getComponentValue(final JSpinner component) {
    return (T) component.getValue();
  }

  @Override
  protected void setComponentValue(final JSpinner component, final T value) {
    component.setValue(value == null ? 0 : value);
  }
}
