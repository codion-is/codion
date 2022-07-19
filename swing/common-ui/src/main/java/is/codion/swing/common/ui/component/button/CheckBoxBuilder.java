/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JCheckBox;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JCheckBox.
 */
public interface CheckBoxBuilder extends ToggleButtonBuilder<JCheckBox, CheckBoxBuilder> {

  /**
   * @param nullable if true then a {@link NullableCheckBox} is built.
   * @return this builder instance
   */
  CheckBoxBuilder nullable(boolean nullable);

  /**
   * @return a builder for a component
   */
  static CheckBoxBuilder builder() {
    return new DefaultCheckBoxBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the check-box
   * @return a builder for a component
   */
  static CheckBoxBuilder builder(Value<Boolean> linkedValue) {
    return new DefaultCheckBoxBuilder(requireNonNull(linkedValue));
  }
}
