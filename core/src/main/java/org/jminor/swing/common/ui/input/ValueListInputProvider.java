/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.common.model.Item;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;

import javax.swing.JComboBox;
import java.util.List;

/**
 * A InputProvider implementation based on a list of Items
 * @param <T> the type represented by the items available via this input provider
 * @see Item
 */
public final class ValueListInputProvider<T> extends AbstractInputProvider<T, JComboBox> {

  /**
   * Instantiates a new ValueListInputProvider.
   * @param initialValue the initial value
   * @param values the available values
   */
  public ValueListInputProvider(final T initialValue, final List<Item<T>> values) {
    super(createComboBox(initialValue, values));
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  @Override
  public T getValue() {
    return ((ItemComboBoxModel<T>) getInputComponent().getModel()).getSelectedValue().getItem();
  }

  private static <T> JComboBox createComboBox(final T currentValue, final List<Item<T>> values) {
    final ItemComboBoxModel<T> boxModel = new ItemComboBoxModel<>(values);
    final JComboBox box = new SteppedComboBox<>(boxModel);
    MaximumMatch.enable(box);
    final Item<T> currentItem = new Item<>(currentValue, "");
    final int currentValueIndex = values.indexOf(currentItem);
    if (currentValueIndex >= 0) {
      boxModel.setSelectedItem(values.get(currentValueIndex));
    }

    return box;
  }
}
