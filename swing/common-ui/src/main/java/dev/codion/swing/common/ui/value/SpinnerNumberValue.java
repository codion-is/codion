/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import javax.swing.SpinnerNumberModel;

final class SpinnerNumberValue<V extends Number> extends AbstractComponentValue<V, SpinnerNumberModel> {

  SpinnerNumberValue(final SpinnerNumberModel spinnerModel) {
    super(spinnerModel);
    spinnerModel.addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected V getComponentValue(final SpinnerNumberModel component) {
    return (V) component.getValue();
  }

  @Override
  protected void setComponentValue(final SpinnerNumberModel component, final V value) {
    component.setValue(value);
  }
}
