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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

final class SelectedItemValue<T, C extends JComboBox<Item<T>>> extends AbstractComponentValue<T, C> {

  SelectedItemValue(C comboBox) {
    super(comboBox);
    component().addItemListener(new NotifyOnItemSelectedListener());
  }

  @Override
  protected T getComponentValue() {
    Item<T> selectedValue = (Item<T>) component().getModel().getSelectedItem();

    return selectedValue == null ? null : selectedValue.get();
  }

  @Override
  protected void setComponentValue(T value) {
    component().getModel().setSelectedItem(value);
  }

  private final class NotifyOnItemSelectedListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyListeners();
      }
    }
  }
}
