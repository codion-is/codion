/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.component.AbstractComponentValue;

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
  protected Boolean getComponentValue() {
    return ((Item<Boolean>) getComponent().getModel().getSelectedItem()).getValue();
  }

  @Override
  protected void setComponentValue(Boolean value) {
    getComponent().setSelectedItem(value);
  }
}
