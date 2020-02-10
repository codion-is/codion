/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Item;
import org.jminor.common.event.EventObserver;
import org.jminor.common.value.Value;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;

import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;

import static org.jminor.common.value.Values.propertyValue;

/**
 * Utility class for boolean {@link ComponentValue} instances.
 */
public final class BooleanValues {

  private BooleanValues() {}

  /**
   * Creates a boolean value based on the given button model.
   * If the button model is a {@link NullableToggleButtonModel} the value will be nullable otherwise not
   * @param buttonModel the button model
   * @return a Value bound to the given button model
   */
  public static ComponentValue<Boolean, ? extends ButtonModel> booleanButtonModelValue(final ButtonModel buttonModel) {
    if (buttonModel instanceof NullableToggleButtonModel) {
      return new BooleanNullableButtonModelValue((NullableToggleButtonModel) buttonModel);
    }

    return new BooleanButtonModelValue(buttonModel);
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

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @return a ButtomModel based on the value
   */
  public static ButtonModel booleanValueLink(final Object owner, final String propertyName, final EventObserver<Boolean> valueChangeEvent) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    booleanValueLink(buttonModel, owner, propertyName, valueChangeEvent);

    return buttonModel;
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void booleanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                      final EventObserver<Boolean> valueChangeEvent) {
    booleanValueLink(buttonModel, propertyValue(owner, propertyName, boolean.class, valueChangeEvent));
  }

  /**
   * @param comboBox the combo box to link with the value
   * @param value the model value
   */
  public static void booleanValueLink(final JComboBox<Item<Boolean>> comboBox, final Value<Boolean> value) {
    value.link(booleanComboBoxValue(comboBox));
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param value the model value
   */
  public static void booleanValueLink(final ButtonModel buttonModel, final Value<Boolean> value) {
    value.link(booleanButtonModelValue(buttonModel));
  }
}
