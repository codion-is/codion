/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JRadioButtonMenuItem;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JRadioButtonMenuItem.
 */
public interface RadioButtonMenuItemBuilder<B extends RadioButtonMenuItemBuilder<B>> extends ToggleMenuItemBuilder<JRadioButtonMenuItem, B> {

  /**
   * @param <B> the builder type
   * @return a builder for a JButton
   */
  static <B extends RadioButtonMenuItemBuilder<B>> RadioButtonMenuItemBuilder<B> builder() {
    return new DefaultRadioButtonMenuItemBuilder<>(null);
  }

  /**
   * @param <B> the builder type
   * @param linkedValue the value to link to the button
   * @return a builder for a JButton
   */
  static <B extends RadioButtonMenuItemBuilder<B>> RadioButtonMenuItemBuilder<B> builder(Value<Boolean> linkedValue) {
    return new DefaultRadioButtonMenuItemBuilder<>(requireNonNull(linkedValue));
  }
}
