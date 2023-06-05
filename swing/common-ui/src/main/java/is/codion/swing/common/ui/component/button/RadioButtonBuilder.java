/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JRadioButton;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JRadioButton.
 */
public interface RadioButtonBuilder extends ToggleButtonBuilder<JRadioButton, RadioButtonBuilder> {

  /**
   * @return a builder for a component
   */
  static RadioButtonBuilder builder() {
    return new DefaultRadioButtonBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the radion button
   * @return a builder for a component
   */
  static RadioButtonBuilder builder(Value<Boolean> linkedValue) {
    return new DefaultRadioButtonBuilder(requireNonNull(linkedValue));
  }

  /**
   * @param toggleControl the toggle control
   * @return a builder for a component
   */
  static RadioButtonBuilder builder(ToggleControl toggleControl) {
    return new DefaultRadioButtonBuilder(requireNonNull(toggleControl), null);
  }

  /**
   * @param toggleControl the toggle control
   * @param linkedValue the value to link to the radio button
   * @return a builder for a component
   */
  static RadioButtonBuilder builder(ToggleControl toggleControl, Value<Boolean> linkedValue) {
    return new DefaultRadioButtonBuilder(requireNonNull(toggleControl), requireNonNull(linkedValue));
  }
}
