/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
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
