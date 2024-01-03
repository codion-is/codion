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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.label;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JLabel;

final class LabelComponentValue<T> extends AbstractComponentValue<T, JLabel> {

  LabelComponentValue(JLabel component) {
    super(component);
  }

  @Override
  protected T getComponentValue() {
    return null;
  }

  @Override
  protected void setComponentValue(T value) {
    component().setText(value == null ? "" : value.toString());
  }
}
