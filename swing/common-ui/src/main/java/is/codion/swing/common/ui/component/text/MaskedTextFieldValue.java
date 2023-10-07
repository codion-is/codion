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
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JFormattedTextField;

final class MaskedTextFieldValue<T> extends AbstractComponentValue<T, JFormattedTextField> {

  MaskedTextFieldValue(JFormattedTextField component) {
    super(component);
    component.addPropertyChangeListener("value", event -> notifyListeners());
  }

  @Override
  protected T getComponentValue() {
    return (T) component().getValue();
  }

  @Override
  protected void setComponentValue(T value) {
    if (value == null) {
      // otherwise the caret goes all the way to the
      // end the next time the field gains focus
      component().setText("");
    }
    else {
      component().setValue(value);
    }
  }
}
