/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.AbstractComponentValue;

final class BooleanNullableCheckBoxValue extends AbstractComponentValue<Boolean, NullableCheckBox> {

  BooleanNullableCheckBoxValue(NullableCheckBox checkBox) {
    super(checkBox);
    checkBox.getNullableModel().addStateListener(value -> notifyValueChange());
  }

  @Override
  protected Boolean getComponentValue() {
    return getComponent().getState();
  }

  @Override
  protected void setComponentValue(Boolean value) {
    getComponent().getNullableModel().setState(value);
  }
}
