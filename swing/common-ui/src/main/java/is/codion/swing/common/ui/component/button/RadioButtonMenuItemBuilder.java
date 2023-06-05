/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JRadioButtonMenuItem;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JRadioButtonMenuItem.
 */
public interface RadioButtonMenuItemBuilder<B extends RadioButtonMenuItemBuilder<B>> extends ToggleMenuItemBuilder<JRadioButtonMenuItem, B> {

  /**
   * @param <B> the builder type
   * @param toggleControl the button action
   * @return a builder for a JButton
   */
  static <B extends RadioButtonMenuItemBuilder<B>> RadioButtonMenuItemBuilder<B> builder(ToggleControl toggleControl) {
    return new DefaultRadioButtonMenuItemBuilder<>(requireNonNull(toggleControl), null);
  }

  /**
   * @param <B> the builder type
   * @param toggleControl the button action
   * @param linkedValue the value to link to the button
   * @return a builder for a JButton
   */
  static <B extends RadioButtonMenuItemBuilder<B>> RadioButtonMenuItemBuilder<B> builder(ToggleControl toggleControl, Value<Boolean> linkedValue) {
    return new DefaultRadioButtonMenuItemBuilder<>(requireNonNull(toggleControl), requireNonNull(linkedValue));
  }
}
