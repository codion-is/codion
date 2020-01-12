/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import javax.swing.BoundedRangeModel;

final class IntegerBoundedRangeModelValue extends AbstractComponentValue<Integer, BoundedRangeModel> {

  IntegerBoundedRangeModelValue(final BoundedRangeModel rangeModel) {
    super(rangeModel, false);
    rangeModel.addChangeListener(e -> notifyValueChange(get()));
  }

  @Override
  public Integer get() {
    return getComponent().getValue();
  }

  @Override
  protected void setInternal(final Integer value) {
    getComponent().setValue(value == null ? 0 : value);
  }
}
