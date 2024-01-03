/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JToggleButton;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JToggleButton.
 * @param <C> the component type
 * @param <B> the builder type
 */
public interface ToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>> extends ButtonBuilder<Boolean, C, B> {

  /**
   * @param toggleControl the toggle control to base this toggle button on
   * @return this builder instance
   */
  B toggleControl(ToggleControl toggleControl);

  /**
   * @param toggleControlBuilder the builder for the toggle control to base this toggle button on
   * @return this builder instance
   */
  B toggleControl(Control.Builder<ToggleControl, ?> toggleControlBuilder);

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
