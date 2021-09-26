/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

final class DefaultItemSpinnerBuilder<T> extends AbstractSpinnerBuilder<T, ItemSpinnerBuilder<T>>
        implements ItemSpinnerBuilder<T> {

  DefaultItemSpinnerBuilder(final SpinnerListModel spinnerModel) {
    super(spinnerModel);
  }

  @Override
  protected ComponentValue<T, JSpinner> buildComponentValue(final JSpinner component) {
    return ComponentValues.itemSpinner(component);
  }
}
