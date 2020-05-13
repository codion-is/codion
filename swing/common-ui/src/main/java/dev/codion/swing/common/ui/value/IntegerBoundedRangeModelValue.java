/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.value;

import dev.codion.common.value.Nullable;

import javax.swing.BoundedRangeModel;

final class IntegerBoundedRangeModelValue extends AbstractComponentValue<Integer, BoundedRangeModel> {

  IntegerBoundedRangeModelValue(final BoundedRangeModel rangeModel) {
    super(rangeModel, Nullable.NO);
    rangeModel.addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected Integer getComponentValue(final BoundedRangeModel component) {
    return component.getValue();
  }

  @Override
  protected void setComponentValue(final BoundedRangeModel component, final Integer value) {
    component.setValue(value == null ? 0 : value);
  }
}
