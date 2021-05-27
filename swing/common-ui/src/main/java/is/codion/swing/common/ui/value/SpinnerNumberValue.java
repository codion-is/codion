/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JSpinner;

final class SpinnerNumberValue<V extends Number> extends AbstractComponentValue<V, JSpinner> {

  SpinnerNumberValue(final JSpinner spinner) {
    super(spinner);
    spinner.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected V getComponentValue(final JSpinner component) {
    return (V) component.getValue();
  }

  @Override
  protected void setComponentValue(final JSpinner component, final V value) {
    component.setValue(value == null ? 0 : value);
  }
}
