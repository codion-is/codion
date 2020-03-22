/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.item.Item;

import javax.swing.JComboBox;
import java.util.List;

/**
 * Utility class for selection {@link ComponentValue} instances.
 */
public final class SelectedValues {

  private SelectedValues() {}

  /**
   * Instantiates a Item based ComponentValue.
   * @param initialValue the initial value
   * @param values the available values
   * @param <V> the value type
   * @return a ComponentValue based on a combo box
   */
  public static <V> ComponentValue<V, JComboBox<Item<V>>> selectedItemValue(final V initialValue, final List<Item<V>> values) {
    return new SelectedItemValue<>(initialValue, values);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box
   * @return a Value bound to the given component
   */
  public static <V> ComponentValue<V, JComboBox<V>> selectedValue(final JComboBox<V> comboBox) {
    return new SelectedValue<>(comboBox);
  }
}
