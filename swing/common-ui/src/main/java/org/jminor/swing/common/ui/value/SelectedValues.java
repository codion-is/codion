/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Item;
import org.jminor.common.event.EventObserver;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;

import javax.swing.JComboBox;
import java.util.List;

import static org.jminor.common.value.Values.propertyValue;

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

  /**
   * @param <V> the value type
   * @param comboBox the combo box to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static <V> void selectedValueLink(final JComboBox<V> comboBox, final Object owner, final String propertyName,
                                           final Class<V> valueClass, final EventObserver<V> valueChangeEvent) {
    selectedValueLink(comboBox, owner, propertyName, valueClass, valueChangeEvent, false);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueClass the value class
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   */
  public static <V> void selectedValueLink(final JComboBox<V> comboBox, final Object owner, final String propertyName,
                                           final Class<V> valueClass, final EventObserver<V> valueChangeEvent,
                                           final boolean readOnly) {
    selectedValueLink(comboBox, propertyValue(owner, propertyName, valueClass, valueChangeEvent), readOnly);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box to link with the value
   * @param value the model value
   */
  public static <V> void selectedValueLink(final JComboBox<V> comboBox, final Value<V> value) {
    Values.link(value, selectedValue(comboBox), false);
  }

  /**
   * @param <V> the value type
   * @param comboBox the combo box to link with the value
   * @param value the model value
   * @param readOnly if true the component will be read only
   */
  public static <V> void selectedValueLink(final JComboBox<V> comboBox, final Value<V> value, final boolean readOnly) {
    Values.link(value, selectedValue(comboBox), readOnly);
  }
}
