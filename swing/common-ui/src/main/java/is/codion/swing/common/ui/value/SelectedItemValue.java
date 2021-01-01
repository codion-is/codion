/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.item.Item;
import is.codion.common.item.Items;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

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
  SelectedItemValue(final JComboBox<Item<T>> comboBox) {
    super(comboBox);
    if (!(comboBox.getModel() instanceof ItemComboBoxModel)) {
      throw new IllegalArgumentException("ComboBoxModel must be a ItemComboBoxModel");
    }
    getComponent().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  /**
   * Instantiates a Item based ComponentValue.
   * @param initialValue the initial value
   * @param values the available values
   */
  SelectedItemValue(final T initialValue, final List<Item<T>> values) {
    super(createComboBox(initialValue, values));
    getComponent().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  @Override
  protected T getComponentValue(final JComboBox<Item<T>> component) {
    final Item<T> selectedValue = ((ItemComboBoxModel<T>) component.getModel()).getSelectedValue();

    return selectedValue == null ? null : selectedValue.getValue();
  }

  @Override
  protected void setComponentValue(final JComboBox<Item<T>> component, final T value) {
    component.getModel().setSelectedItem(value);
  }

  private static <T> JComboBox<Item<T>> createComboBox(final T currentValue, final List<Item<T>> values) {
    final ItemComboBoxModel<T> boxModel = new ItemComboBoxModel<>(values);
    final JComboBox<Item<T>> box = Completion.maximumMatch(new SteppedComboBox<>(boxModel));
    final Item<T> currentItem = Items.item(currentValue, "");
    final int currentValueIndex = values.indexOf(currentItem);
    if (currentValueIndex >= 0) {
      boxModel.setSelectedItem(values.get(currentValueIndex));
    }

    return box;
  }
}
