/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Item;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.util.List;

/**
 * A ComponentValue implementation based on a list of Items
 * @param <T> the type represented by the items available via this input provider
 * @see Item
 */
final class SelectedItemValue<T> extends AbstractComponentValue<T, JComboBox<Item<T>>> {

  /**
   * Instantiates a Item based ComponentValue.
   * @param initialValue the initial value
   * @param values the available values
   */
  SelectedItemValue(final T initialValue, final List<Item<T>> values) {
    super(createComboBox(initialValue, values));
    getComponent().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange(get());
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  public T get() {
    return ((ItemComboBoxModel<T>) getComponent().getModel()).getSelectedValue().getValue();
  }

  /** {@inheritDoc} */
  @Override
  protected void setComponentValue(final T value) {
    getComponent().setSelectedItem(value);
  }

  private static <T> JComboBox<Item<T>> createComboBox(final T currentValue, final List<Item<T>> values) {
    final ItemComboBoxModel<T> boxModel = new ItemComboBoxModel<>(values);
    final JComboBox<Item<T>> box = new SteppedComboBox<>(boxModel);
    MaximumMatch.enable(box);
    final Item<T> currentItem = new Item<>(currentValue, "");
    final int currentValueIndex = values.indexOf(currentItem);
    if (currentValueIndex >= 0) {
      boxModel.setSelectedItem(values.get(currentValueIndex));
    }

    return box;
  }
}
