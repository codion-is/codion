/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

final class DefaultItemSpinnerBuilder<T> extends AbstractSpinnerBuilder<T, ItemSpinnerBuilder<T>>
        implements ItemSpinnerBuilder<T> {

  DefaultItemSpinnerBuilder(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    super(spinnerModel, linkedValue);
  }

  @Override
  protected ComponentValue<T, JSpinner> createComponentValue(JSpinner component) {
    return new SpinnerItemValue<>(component);
  }
}
