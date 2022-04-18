/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JToggleButton;

final class BooleanToggleButtonValue extends AbstractComponentValue<Boolean, JToggleButton> {

  BooleanToggleButtonValue(JToggleButton button) {
    super(button, false);
    button.getModel().addItemListener(itemEvent -> notifyValueChange());
  }

  @Override
  protected Boolean getComponentValue(JToggleButton component) {
    return component.isSelected();
  }

  @Override
  protected void setComponentValue(JToggleButton component, Boolean value) {
     component.setSelected(value != null && value);
  }
}
