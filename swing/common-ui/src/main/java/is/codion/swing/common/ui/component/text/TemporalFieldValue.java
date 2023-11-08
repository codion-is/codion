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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.temporal.Temporal;

final class TemporalFieldValue<T extends Temporal> extends AbstractComponentValue<T, TemporalField<T>> {

  TemporalFieldValue(TemporalField<T> component, UpdateOn updateOn) {
    super(component);
    if (updateOn == UpdateOn.VALUE_CHANGE) {
      component.addListener(value -> notifyListeners());
    }
    else {
      component.addFocusListener(new NotifyOnFocusLost());
    }
  }

  @Override
  protected T getComponentValue() {
    return component().getTemporal();
  }

  @Override
  protected void setComponentValue(T value) {
    component().setTemporal(value);
  }

  private final class NotifyOnFocusLost extends FocusAdapter {
    @Override
    public void focusLost(FocusEvent e) {
      if (!e.isTemporary()) {
        notifyListeners();
      }
    }
  }
}
