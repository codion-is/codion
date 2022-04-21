/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JRadioButton;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JRadioButton.
 */
public interface RadioButtonBuilder extends ToggleButtonBuilder<JRadioButton, RadioButtonBuilder> {

  /**
   * @param horizontalAlignment the horizontal alignment
   * @return this builder instance
   */
  RadioButtonBuilder horizontalAlignment(int horizontalAlignment);

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
}