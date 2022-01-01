/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.checkbox.NullableCheckBox;

final class BooleanNullableCheckBoxValue extends AbstractComponentValue<Boolean, NullableCheckBox> {

  BooleanNullableCheckBoxValue(final NullableCheckBox checkBox) {
    super(checkBox);
    checkBox.getNullableModel().addStateListener(value -> notifyValueChange());
  }

  @Override
  protected Boolean getComponentValue(final NullableCheckBox component) {
    return component.getState();
  }

  @Override
  protected void setComponentValue(final NullableCheckBox component, final Boolean value) {
    component.getNullableModel().setState(value);
  }
}
