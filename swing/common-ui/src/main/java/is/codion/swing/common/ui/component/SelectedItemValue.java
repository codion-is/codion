/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

final class SelectedItemValue<T, C extends JComboBox<Item<T>>> extends AbstractComponentValue<T, C> {

  SelectedItemValue(final C comboBox) {
    super(comboBox);
    getComponent().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  @Override
  protected T getComponentValue(final C component) {
    Item<T> selectedValue = (Item<T>) component.getModel().getSelectedItem();

    return selectedValue == null ? null : selectedValue.getValue();
  }

  @Override
  protected void setComponentValue(final C component, final T value) {
    component.getModel().setSelectedItem(value);
  }
}
