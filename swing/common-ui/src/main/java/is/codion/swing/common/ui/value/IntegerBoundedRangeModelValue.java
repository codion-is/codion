/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.BoundedRangeModel;

final class IntegerBoundedRangeModelValue extends AbstractComponentValue<Integer, BoundedRangeModel> {

  IntegerBoundedRangeModelValue(final BoundedRangeModel rangeModel) {
    super(rangeModel, 0);
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
