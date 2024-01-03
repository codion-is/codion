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
package is.codion.swing.common.ui.component.combobox;

import javax.swing.ComboBoxModel;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A mouse wheel listener for JComboBox, moving to the next or previous value on wheel spin.
 */
final class ComboBoxMouseWheelListener implements MouseWheelListener {

  private final ComboBoxModel<?> comboBoxModel;
  private final boolean wrapAround;

  ComboBoxMouseWheelListener(ComboBoxModel<?> comboBoxModel, boolean wrapAround) {
    this.comboBoxModel = requireNonNull(comboBoxModel);
    this.wrapAround = wrapAround;
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent event) {
    if (comboBoxModel.getSize() == 0) {
      return;
    }
    int wheelRotation = event.getWheelRotation();
    if (wheelRotation != 0) {
      comboBoxModel.setSelectedItem(itemToSelect(wheelRotation > 0));
    }
  }

  private Object itemToSelect(boolean next) {
    Object currentSelection = comboBoxModel.getSelectedItem();
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      if (Objects.equals(currentSelection, comboBoxModel.getElementAt(i))) {
        return comboBoxModel.getElementAt(next ? nextIndex(i) : previousIndex(i));
      }
    }

    return comboBoxModel.getElementAt(next ? nextIndex(0) : previousIndex(0));
  }

  private int nextIndex(int currentIndex) {
    return currentIndex == comboBoxModel.getSize() - 1 ? (wrapAround ? 0 : currentIndex) : currentIndex + 1;
  }

  private int previousIndex(int currentIndex) {
    return currentIndex == 0 ? (wrapAround ? comboBoxModel.getSize() - 1 : currentIndex) : currentIndex - 1;
  }
}
