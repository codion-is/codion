/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JSpinner;
import javax.swing.text.DefaultFormatter;

final class SpinnerNumberValue<T extends Number> extends AbstractComponentValue<T, JSpinner> {

  SpinnerNumberValue(JSpinner spinner, boolean commitOnValidEdit) {
    super(spinner);
    spinner.getModel().addChangeListener(e -> notifyValueChange());
    if (commitOnValidEdit) {
      ((DefaultFormatter) ((JSpinner.NumberEditor) spinner.getEditor())
              .getTextField().getFormatter()).setCommitsOnValidEdit(true);
    }
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
