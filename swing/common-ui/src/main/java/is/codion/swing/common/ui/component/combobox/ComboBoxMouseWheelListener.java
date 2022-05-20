/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import javax.swing.ComboBoxModel;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import static java.util.Objects.requireNonNull;

/**
 * A mouse wheel listener for JComboBox, moving to the next or previous value on wheel spin.
 * @see #create(ComboBoxModel)
 * @see #createWithWrapAround(ComboBoxModel)
 */
public final class ComboBoxMouseWheelListener implements MouseWheelListener {

  private final ComboBoxModel<?> comboBoxModel;
  private final boolean wrapAround;

  private ComboBoxMouseWheelListener(ComboBoxModel<?> comboBoxModel, boolean wrapAround) {
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
      comboBoxModel.setSelectedItem(getItemToSelect(wheelRotation > 0));
    }
  }

  /**
   * Instantiates a new mouse wheel listener for the given combo box model
   * @param comboBoxModel the combo box model
   * @param <T> the combo box value type
   * @return a new MouseWheelListener based on the given model
   */
  public static <T> MouseWheelListener create(ComboBoxModel<T> comboBoxModel) {
    return new ComboBoxMouseWheelListener(comboBoxModel, false);
  }

  /**
   * Instantiates a new mouse wheel listener with wrap-around for the given combo box model
   * @param comboBoxModel the combo box model
   * @param <T> the combo box value type
   * @return a new MouseWheelListener based on the given model
   */
  public static <T> MouseWheelListener createWithWrapAround(ComboBoxModel<T> comboBoxModel) {
    return new ComboBoxMouseWheelListener(comboBoxModel, true);
  }

  private Object getItemToSelect(boolean next) {
    Object currentSelection = comboBoxModel.getSelectedItem();
    if (currentSelection == null) {
      return next || wrapAround ? comboBoxModel.getElementAt(0) : null;
    }
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      if (currentSelection == comboBoxModel.getElementAt(i)) {
        return comboBoxModel.getElementAt(next ? getNextIndex(i) : getPreviousIndex(i));
      }
    }

    return currentSelection;
  }

  private int getNextIndex(int currentIndex) {
    return currentIndex == comboBoxModel.getSize() - 1 ? (wrapAround ? 0 : currentIndex) : currentIndex + 1;
  }

  private int getPreviousIndex(int currentIndex) {
    return currentIndex == 0 ? (wrapAround ? comboBoxModel.getSize() - 1 : currentIndex) : currentIndex - 1;
  }
}
