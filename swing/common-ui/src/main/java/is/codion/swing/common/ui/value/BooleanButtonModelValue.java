/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.ButtonModel;

final class BooleanButtonModelValue extends AbstractComponentValue<Boolean, ButtonModel> {

  BooleanButtonModelValue(final ButtonModel buttonModel) {
    super(buttonModel, false);
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
