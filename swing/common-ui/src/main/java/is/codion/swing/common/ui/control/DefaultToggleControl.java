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

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

import java.awt.event.ActionEvent;

import static java.util.Objects.requireNonNull;

/**
 * A default ToggleControl implementation.
 */
final class DefaultToggleControl extends AbstractControl implements ToggleControl {

  private final Value<Boolean> value;

  /**
   * @param value the value to toggle
   * @param name the name
   * @param enabled an observer indicating when this control should be enabled
   */
  DefaultToggleControl(Value<Boolean> value, String name, StateObserver enabled) {
    super(name, enabled);
    this.value = requireNonNull(value, "value");
  }

  @Override
  public Value<Boolean> value() {
    return value;
  }

  @Override
  public void actionPerformed(ActionEvent e) {/*Not required*/}
}
