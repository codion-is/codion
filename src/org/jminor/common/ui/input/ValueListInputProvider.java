/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.model.Item;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;

import javax.swing.JComboBox;
import java.util.List;

/**
 * A InputProvider implementation based on a list of Item's
 * @see Item
 */
public final class ValueListInputProvider extends AbstractInputProvider<Object, JComboBox> {

  /**
   * Instantiates a new ValueListInputProvider.
   * @param initialValue the initial value
   * @param values the available values
   */
  public ValueListInputProvider(final Object initialValue, final List<Item<Object>> values) {
    super(createComboBox(initialValue, values));
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  @Override
  public Object getValue() {
    return ((ItemComboBoxModel<Object>) getInputComponent().getModel()).getSelectedValue().getItem();
  }

  private static JComboBox createComboBox(final Object currentValue, final List<Item<Object>> values) {
    final ItemComboBoxModel<Object> boxModel = new ItemComboBoxModel<Object>(values);
    final JComboBox box = new SteppedComboBox(boxModel);
    MaximumMatch.enable(box);
    final Item<Object> currentItem = new Item<Object>(currentValue, "");
    final int currentValueIndex = values.indexOf(currentItem);
    if (currentValueIndex >= 0) {
      boxModel.setSelectedItem(values.get(currentValueIndex));
    }

    return box;
  }
}
