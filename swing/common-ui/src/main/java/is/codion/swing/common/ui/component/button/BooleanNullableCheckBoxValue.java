/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

final class BooleanNullableCheckBoxValue extends AbstractComponentValue<Boolean, NullableCheckBox> {

  BooleanNullableCheckBoxValue(NullableCheckBox checkBox) {
    super(checkBox);
    checkBox.getNullableModel().addListener(value -> notifyValueChange());
  }

  @Override
  protected Boolean getComponentValue() {
    return component().getState();
  }

  @Override
  protected void setComponentValue(Boolean value) {
    component().getNullableModel().setState(value);
  }
}
