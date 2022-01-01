/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

final class DefaultItemSpinnerBuilder<T> extends AbstractSpinnerBuilder<T, ItemSpinnerBuilder<T>>
        implements ItemSpinnerBuilder<T> {

  DefaultItemSpinnerBuilder(final SpinnerListModel spinnerModel, final Value<T> linkedValue) {
    super(spinnerModel, linkedValue);
  }

  @Override
  protected ComponentValue<T, JSpinner> buildComponentValue(final JSpinner component) {
    return ComponentValues.itemSpinner(component);
  }
}
