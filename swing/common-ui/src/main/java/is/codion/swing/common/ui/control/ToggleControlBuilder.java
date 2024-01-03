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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.value.Value;

import static java.util.Objects.requireNonNull;

final class ToggleControlBuilder<B extends Control.Builder<ToggleControl, B>> extends AbstractControlBuilder<ToggleControl, B> {

  private final Value<Boolean> value;

  ToggleControlBuilder(Value<Boolean> value) {
    this.value = requireNonNull(value);
  }

  @Override
  protected ToggleControl createControl() {
    return new DefaultToggleControl(value, name, enabled);
  }
}
