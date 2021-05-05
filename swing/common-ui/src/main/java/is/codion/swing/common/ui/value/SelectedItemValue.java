/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.item.Item;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

/**
 * A ComponentValue implementation based on a list of Items
 * @param <V> the type represented by the items available via this input provider
 * @see Item
 */
final class SelectedItemValue<V, C extends JComboBox<Item<V>>> extends AbstractComponentValue<V, C> {

  /**
   * Instantiates a Item based ComponentValue.
   * @param initialValue the initial value
   * @param values the available values
   */
  SelectedItemValue(final C comboBox) {
    super(comboBox);
    getComponent().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  @Override
  protected V getComponentValue(final C component) {
    final Item<V> selectedValue = (Item<V>) component.getModel().getSelectedItem();

    return selectedValue == null ? null : selectedValue.getValue();
  }

  @Override
  protected void setComponentValue(final C component, final V value) {
    component.getModel().setSelectedItem(value);
  }
}
