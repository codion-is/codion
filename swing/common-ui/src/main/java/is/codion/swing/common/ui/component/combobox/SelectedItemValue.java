/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

final class SelectedItemValue<T, C extends JComboBox<Item<T>>> extends AbstractComponentValue<T, C> {

  SelectedItemValue(C comboBox) {
    super(comboBox);
    getComponent().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  @Override
  protected T getComponentValue(C component) {
    Item<T> selectedValue = (Item<T>) component.getModel().getSelectedItem();

    return selectedValue == null ? null : selectedValue.getValue();
  }

  @Override
  protected void setComponentValue(C component, T value) {
    component.getModel().setSelectedItem(value);
  }
}
