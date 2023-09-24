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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * A Control for toggling a boolean value.
 */
public interface ToggleControl extends Control {

  /**
   * @return the value being toggled by this toggle control
   */
  Value<Boolean> value();

  /**
   * Creates a new ToggleControl based on the given value
   * @param value the value
   * @return a new ToggleControl
   */
  static ToggleControl toggleControl(Value<Boolean> value) {
    return builder(value).build();
  }

  /**
   * Creates a new ToggleControl based on the given state
   * @param state the state
   * @return a new ToggleControl
   */
  static ToggleControl toggleControl(State state) {
    return builder(state).build();
  }

  /**
   * @param value the value to toggle
   * @return a new ToggleControl.Builder
   */
  static Builder builder(Value<Boolean> value) {
    return new ToggleControlBuilder(value);
  }

  /**
   * @param state the state to toggle
   * @return a new ToggleControl.Builder
   */
  static Builder builder(State state) {
    return new ToggleControlBuilder(state);
  }

  /**
   * A builder for ToggleControl
   */
  interface Builder extends Control.Builder {

    /**
     * @param name the name of the control
     * @return this Builder instance
     */
    Builder name(String name);

    /**
     * @param enabled the state observer which controls the enabled state of the control
     * @return this Builder instance
     */
    Builder enabled(StateObserver enabled);

    /**
     * @param mnemonic the control mnemonic
     * @return this Builder instance
     */
    Builder mnemonic(char mnemonic);

    /**
     * @param smallIcon the small control icon
     * @return this Builder instance
     */
    Builder smallIcon(Icon smallIcon);

    /**
     * @param description a string describing the control
     * @return this Builder instance
     */
    Builder description(String description);

    /**
     * @param keyStroke the keystroke to associate with the control
     * @return this Builder instance
     */
    Builder keyStroke(KeyStroke keyStroke);

    /**
     * @return a new ToggleControl
     */
    ToggleControl build();
  }
}
