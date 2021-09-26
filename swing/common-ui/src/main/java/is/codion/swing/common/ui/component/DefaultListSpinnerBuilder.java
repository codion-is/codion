/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

final class DefaultListSpinnerBuilder<T> extends AbstractSpinnerBuilder<T, ListSpinnerBuilder<T>>
        implements ListSpinnerBuilder<T> {

  DefaultListSpinnerBuilder(final SpinnerListModel spinnerModel) {
    super(spinnerModel);
  }

  @Override
  protected ComponentValue<T, JSpinner> buildComponentValue(final JSpinner component) {
    return ComponentValues.listSpinner(component);
  }
}
