/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

final class BooleanComboBoxValue extends AbstractComponentValue<Boolean, JComboBox<Item<Boolean>>> {

  /**
   * Instantiates a new {@link BooleanComboBoxValue}.
   * @param comboBox the combo box
   */
  BooleanComboBoxValue(JComboBox<Item<Boolean>> comboBox) {
    super(comboBox);
    getComponent().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  @Override
  protected Boolean getComponentValue(JComboBox<Item<Boolean>> component) {
    return ((Item<Boolean>) component.getModel().getSelectedItem()).getValue();
  }

  @Override
  protected void setComponentValue(JComboBox<Item<Boolean>> component, Boolean value) {
    component.setSelectedItem(value);
  }
}
