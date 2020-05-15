/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.item.Item;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

final class BooleanComboBoxValue extends AbstractComponentValue<Boolean, JComboBox<Item<Boolean>>> {

  /**
   * Instantiates a new {@link BooleanComboBoxValue}.
   * @param comboBox the combo box
   */
  BooleanComboBoxValue(final JComboBox<Item<Boolean>> comboBox) {
    super(comboBox);
    getComponent().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  @Override
  protected Boolean getComponentValue(final JComboBox<Item<Boolean>> component) {
    return ((Item<Boolean>) component.getModel().getSelectedItem()).getValue();
  }

  @Override
  protected void setComponentValue(final JComboBox<Item<Boolean>> component, final Boolean value) {
    component.setSelectedItem(value);
  }
}
