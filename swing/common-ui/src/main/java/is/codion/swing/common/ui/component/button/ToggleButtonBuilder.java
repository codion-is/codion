/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JToggleButton;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JToggleButton.
 */
public interface ToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>> extends ButtonBuilder<Boolean, C, B> {

  /**
   * @param <C> the component type
   * @param <B> the builder type
   * @return a builder for a component
   */
  static <C extends JToggleButton, B extends ToggleButtonBuilder<C, B>> ToggleButtonBuilder<C, B> builder() {
    return new DefaultToggleButtonBuilder<>(null);
  }

  /**
   * @param linkedValue the value to link to the button
   * @param <C> the component type
   * @param <B> the builder type
   * @return a builder for a component
   */
  static <C extends JToggleButton, B extends ToggleButtonBuilder<C, B>> ToggleButtonBuilder<C, B> builder(Value<Boolean> linkedValue) {
    return new DefaultToggleButtonBuilder<>(requireNonNull(linkedValue));
  }
}
