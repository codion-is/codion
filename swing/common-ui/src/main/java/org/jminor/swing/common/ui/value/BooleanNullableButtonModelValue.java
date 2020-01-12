/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;

final class BooleanNullableButtonModelValue extends AbstractComponentValue<Boolean, NullableToggleButtonModel> {

  BooleanNullableButtonModelValue(final NullableToggleButtonModel buttonModel) {
    super(buttonModel);
    buttonModel.addStateListener(value -> notifyValueChange(get()));
  }

  @Override
  protected Boolean getComponentValue(final NullableToggleButtonModel component) {
    return component.getState();
  }

  @Override
  protected void setComponentValue(final NullableToggleButtonModel component, final Boolean value) {
    component.setState(value);
  }
}
