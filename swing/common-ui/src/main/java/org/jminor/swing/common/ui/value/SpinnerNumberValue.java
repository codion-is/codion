/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import javax.swing.SpinnerNumberModel;

final class SpinnerNumberValue<V extends Number> extends AbstractComponentValue<V, SpinnerNumberModel> {

  SpinnerNumberValue(final SpinnerNumberModel spinnerModel) {
    super(spinnerModel);
    spinnerModel.addChangeListener(e -> notifyValueChange(get()));
  }

  @Override
  public V get() {
    return (V) getComponent().getValue();
  }

  @Override
  protected void setInternal(final V value) {
    getComponent().setValue(value);
  }
}
