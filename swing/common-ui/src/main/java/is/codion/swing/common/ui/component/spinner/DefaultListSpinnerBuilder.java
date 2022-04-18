/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

final class DefaultListSpinnerBuilder<T> extends AbstractSpinnerBuilder<T, ListSpinnerBuilder<T>>
        implements ListSpinnerBuilder<T> {

  DefaultListSpinnerBuilder(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    super(spinnerModel, linkedValue);
  }

  @Override
  protected ComponentValue<T, JSpinner> createComponentValue(JSpinner component) {
    return new SpinnerListValue<>(component);
  }
}
