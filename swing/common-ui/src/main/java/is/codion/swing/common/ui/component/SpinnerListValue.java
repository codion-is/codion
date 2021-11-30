/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

final class SpinnerListValue<T> extends AbstractComponentValue<T, JSpinner> {

  SpinnerListValue(final JSpinner spinner) {
    super(spinner);
    if (!(spinner.getModel() instanceof SpinnerListModel)) {
      throw new IllegalArgumentException("Spinner model must be a SpinnerListModel");
    }
    spinner.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected T getComponentValue(final JSpinner component) {
    return (T) component.getValue();
  }

  @Override
  protected void setComponentValue(final JSpinner component, final T value) {
    component.setValue(value == null ? ((SpinnerListModel) component.getModel()).getList().get(0) : value);
  }
}
