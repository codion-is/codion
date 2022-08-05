/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

final class SelectedItemValue<T, C extends JComboBox<Item<T>>> extends AbstractComponentValue<T, C> {

  SelectedItemValue(C comboBox) {
    super(comboBox);
    getComponent().addItemListener(new NotifyOnItemSelectedListener());
  }

  @Override
  protected T getComponentValue() {
    Item<T> selectedValue = (Item<T>) getComponent().getModel().getSelectedItem();

    return selectedValue == null ? null : selectedValue.value();
  }

  @Override
  protected void setComponentValue(T value) {
    getComponent().getModel().setSelectedItem(value);
  }

  private final class NotifyOnItemSelectedListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    }
  }
}
