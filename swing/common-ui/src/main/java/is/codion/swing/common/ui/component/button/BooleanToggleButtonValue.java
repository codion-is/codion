/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JToggleButton;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

final class BooleanToggleButtonValue extends AbstractComponentValue<Boolean, JToggleButton> {

  BooleanToggleButtonValue(JToggleButton button) {
    super(button, false);
    button.getModel().addItemListener(new NotifyOnItemEvent());
  }

  @Override
  protected Boolean getComponentValue() {
    return component().isSelected();
  }

  @Override
  protected void setComponentValue(Boolean value) {
    component().setSelected(value != null && value);
  }

  private final class NotifyOnItemEvent implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent itemEvent) {
      notifyValueChange();
    }
  }
}
