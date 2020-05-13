/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.value;

import dev.codion.common.value.Nullable;

import javax.swing.ButtonModel;

final class BooleanButtonModelValue extends AbstractComponentValue<Boolean, ButtonModel> {

  BooleanButtonModelValue(final ButtonModel buttonModel) {
    super(buttonModel, Nullable.NO);
    buttonModel.addItemListener(itemEvent -> notifyValueChange());
  }

  @Override
  protected Boolean getComponentValue(final ButtonModel component) {
    return component.isSelected();
  }

  @Override
  protected void setComponentValue(final ButtonModel component, final Boolean value) {
     component.setSelected(value != null && value);
  }
}
