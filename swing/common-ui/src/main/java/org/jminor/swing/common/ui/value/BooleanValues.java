/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Item;
import org.jminor.common.event.EventObserver;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;

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
   * Instantiates a new Boolean based ComponentValue.
   * @param initialValue the initial value
   * @return a Boolean based ComponentValue
   */
  public static ComponentValue<Boolean, JComboBox<Item<Boolean>>> booleanComboBoxValue(final Boolean initialValue) {
    return new BooleanComboBoxValue(initialValue);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @return a ButtomModel based on the value
   */
  public static ButtonModel toggleValueLink(final Object owner, final String propertyName, final EventObserver<Boolean> valueChangeEvent) {
    return toggleValueLink(owner, propertyName, valueChangeEvent, false);
  }

  /**
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   * @return a ButtomModel based on the value
   */
  public static ButtonModel toggleValueLink(final Object owner, final String propertyName, final EventObserver<Boolean> valueChangeEvent,
                                            final boolean readOnly) {
    final ButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
    toggleValueLink(buttonModel, owner, propertyName, valueChangeEvent, readOnly);

    return buttonModel;
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                     final EventObserver<Boolean> valueChangeEvent) {
    toggleValueLink(buttonModel, owner, propertyName, valueChangeEvent, false);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param readOnly if true the component will be read only
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                     final EventObserver<Boolean> valueChangeEvent, final boolean readOnly) {
    toggleValueLink(buttonModel, propertyValue(owner, propertyName, boolean.class, valueChangeEvent), readOnly);
  }

  /**
   * @param buttonModel the button model to link with the value
   * @param value the model value
   * @param readOnly if true the component will be read only
   */
  public static void toggleValueLink(final ButtonModel buttonModel, final Value<Boolean> value, final boolean readOnly) {
    Values.link(value, booleanButtonModelValue(buttonModel), readOnly);
  }
}
