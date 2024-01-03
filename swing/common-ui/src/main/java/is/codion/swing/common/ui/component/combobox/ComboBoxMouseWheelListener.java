/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
