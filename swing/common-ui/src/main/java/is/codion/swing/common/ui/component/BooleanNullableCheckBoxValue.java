/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.checkbox.NullableCheckBox;

final class BooleanNullableCheckBoxValue extends AbstractComponentValue<Boolean, NullableCheckBox> {

  BooleanNullableCheckBoxValue(NullableCheckBox checkBox) {
    super(checkBox);
    checkBox.getNullableModel().addStateListener(value -> notifyValueChange());
  }

  @Override
  protected Boolean getComponentValue(NullableCheckBox component) {
    return component.getState();
  }

  @Override
  protected void setComponentValue(NullableCheckBox component, Boolean value) {
    component.getNullableModel().setState(value);
  }
}
