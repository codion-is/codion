/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.combobox;

import javax.swing.ComboBoxModel;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import static java.util.Objects.requireNonNull;

/**
 * A simple mouse wheel listener for JComboBox, moving to the next or previous value on wheel spin.
 */
public final class ComboBoxMouseWheelListener implements MouseWheelListener {

  private final ComboBoxModel<?> comboBoxModel;

  /**
   * Instantiates a new mouse wheel listener
   * @param comboBoxModel the combo box model
   */
  public ComboBoxMouseWheelListener(final ComboBoxModel<?> comboBoxModel) {
    this.comboBoxModel = requireNonNull(comboBoxModel);
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent event) {
    if (comboBoxModel.getSize() < 2) {
      return;
    }
    final int wheelRotation = event.getWheelRotation();
    if (wheelRotation != 0) {
      comboBoxModel.setSelectedItem(getItemToSelect(wheelRotation > 0));
    }
  }

  private Object getItemToSelect(final boolean next) {
    final Object currentSelection = comboBoxModel.getSelectedItem();
    if (currentSelection == null) {
      return next ? comboBoxModel.getElementAt(0) : comboBoxModel.getElementAt(comboBoxModel.getSize() - 1);
    }
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      if (currentSelection == comboBoxModel.getElementAt(i)) {
        return next ? getNext(i) : getPrevious(i);
      }
    }

    return currentSelection;
  }

  private Object getNext(final int currentIndex) {
    return currentIndex == comboBoxModel.getSize() - 1 ?
            comboBoxModel.getElementAt(0) : comboBoxModel.getElementAt(currentIndex + 1);
  }

  private Object getPrevious(final int currentIndex) {
    return currentIndex == 0 ?
            comboBoxModel.getElementAt(comboBoxModel.getSize() - 1) : comboBoxModel.getElementAt(currentIndex - 1);
  }
}
