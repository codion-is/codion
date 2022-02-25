/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

final class DefaultListSpinnerBuilder<T> extends AbstractSpinnerBuilder<T, ListSpinnerBuilder<T>>
        implements ListSpinnerBuilder<T> {

  DefaultListSpinnerBuilder(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    super(spinnerModel, linkedValue);
  }

  @Override
  protected ComponentValue<T, JSpinner> buildComponentValue(JSpinner component) {
    return ComponentValues.listSpinner(component);
  }
}
