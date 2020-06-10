/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.item.Item;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;

import javax.swing.ButtonModel;
import javax.swing.JComboBox;

/**
 * Utility class for boolean {@link ComponentValue} instances.
 */
public final class BooleanValues {

  private BooleanValues() {}

  /**
   * Creates a boolean value based on the given button model.
   * If the button model is a {@link NullableToggleButtonModel} the value will be nullable otherwise not
   * @param buttonModel the button model
   * @param <T> the attribute type
   * @return a Value bound to the given button model
   */
  public static <T extends ButtonModel> ComponentValue<Boolean, T> booleanButtonModelValue(final ButtonModel buttonModel) {
    if (buttonModel instanceof NullableToggleButtonModel) {
      return (ComponentValue<Boolean, T>) new BooleanNullableButtonModelValue((NullableToggleButtonModel) buttonModel);
    }

    return (ComponentValue<Boolean, T>) new BooleanButtonModelValue(buttonModel);
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
