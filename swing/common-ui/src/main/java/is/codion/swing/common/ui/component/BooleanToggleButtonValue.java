/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JToggleButton;

final class BooleanToggleButtonValue extends AbstractComponentValue<Boolean, JToggleButton> {

  BooleanToggleButtonValue(final JToggleButton button) {
    super(button, false);
    button.getModel().addItemListener(itemEvent -> notifyValueChange());
  }

  @Override
  protected Boolean getComponentValue(final JToggleButton component) {
    return component.isSelected();
  }

  @Override
  protected void setComponentValue(final JToggleButton component, final Boolean value) {
     component.setSelected(value != null && value);
  }
}
