/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

final class SpinnerListValue<V> extends AbstractComponentValue<V, JSpinner> {

  SpinnerListValue(final JSpinner spinner) {
    super(spinner);
    if (!(spinner.getModel() instanceof SpinnerListModel)) {
      throw new IllegalArgumentException("Spinner model must be a SpinnerListModel");
    }
    spinner.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected V getComponentValue(final JSpinner component) {
    return (V) component.getValue();
  }

  @Override
  protected void setComponentValue(final JSpinner component, final V value) {
    component.setValue(value == null ? ((SpinnerListModel) component.getModel()).getList().get(0) : value);
  }
}
