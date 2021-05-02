/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.item.Item;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;

import javax.swing.JComboBox;
import javax.swing.JToggleButton;

/**
 * Utility class for boolean {@link ComponentValue} instances.
 */
public final class BooleanValues {

  private BooleanValues() {}

  /**
   * Creates a boolean value based on the given toggle button.
   * If the button is a {@link NullableCheckBox} the value will be nullable otherwise not
   * @param button the button
   * @param <T> the attribute type
   * @return a Value bound to the given button
   */
  public static <T extends JToggleButton> ComponentValue<Boolean, T> booleanToggleButtonValue(final JToggleButton button) {
    if (button instanceof NullableCheckBox) {
      return (ComponentValue<Boolean, T>) new BooleanNullableCheckBoxValue((NullableCheckBox) button);
    }

    return (ComponentValue<Boolean, T>) new BooleanToggleButtonValue(button);
  }

  /**
   * Instantiates a new Boolean based ComponentValue with a null initial value.
   * @return a Boolean based ComponentValue
   */
  public static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBoxValue() {
    return booleanComboBoxValue((Boolean) null);
  }

  /**
   * Instantiates a new Boolean based ComponentValue.
   * @param initialValue the initial value
   * @return a Boolean based ComponentValue
   */
  public static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBoxValue(final Boolean initialValue) {
    final BooleanComboBoxModel model = new BooleanComboBoxModel();
    model.setSelectedItem(initialValue);

    return booleanComboBoxValue(new JComboBox<>(model));
  }

  /**
   * Instantiates a new Boolean based ComponentValue.
   * @param comboBox the combo box
   * @return a Boolean based ComponentValue
   */
  public static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBoxValue(final JComboBox<Item<Boolean>> comboBox) {
    return new BooleanComboBoxValue(comboBox);
  }
}
