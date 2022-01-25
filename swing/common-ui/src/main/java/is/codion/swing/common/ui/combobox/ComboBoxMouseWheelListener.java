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
 * @see #create(ComboBoxModel)
 * @see #createWithWrapAround(ComboBoxModel)
 */
public final class ComboBoxMouseWheelListener implements MouseWheelListener {

  private final ComboBoxModel<?> comboBoxModel;
  private final boolean wrapAround;

  private ComboBoxMouseWheelListener(final ComboBoxModel<?> comboBoxModel, final boolean wrapAround) {
    this.comboBoxModel = requireNonNull(comboBoxModel);
    this.wrapAround = wrapAround;
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent event) {
    if (comboBoxModel.getSize() == 0) {
      return;
    }
    final int wheelRotation = event.getWheelRotation();
    if (wheelRotation != 0) {
      comboBoxModel.setSelectedItem(getItemToSelect(wheelRotation > 0));
    }
  }

  /**
   * Instantiates a new mouse wheel listener for the given combo boxe model
   * @param comboBoxModel the combo box model
   * @param <T> the combo box value type
   * @return a new MouseWheelListener based on the given model
   */
  public static <T> MouseWheelListener create(final ComboBoxModel<T> comboBoxModel) {
    return new ComboBoxMouseWheelListener(comboBoxModel, false);
  }

  /**
   * Instantiates a new mouse wheel listener with wrap-around for the given combo boxe model
   * @param comboBoxModel the combo box model
   * @param <T> the combo box value type
   * @return a new MouseWheelListener based on the given model
   */
  public static <T> MouseWheelListener createWithWrapAround(final ComboBoxModel<T> comboBoxModel) {
    return new ComboBoxMouseWheelListener(comboBoxModel, true);
  }

  private Object getItemToSelect(final boolean next) {
    final Object currentSelection = comboBoxModel.getSelectedItem();
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

  private int getNextIndex(final int currentIndex) {
    return currentIndex == comboBoxModel.getSize() - 1 ? (wrapAround ? 0 : currentIndex) : currentIndex + 1;
  }

  private int getPreviousIndex(final int currentIndex) {
    return currentIndex == 0 ? (wrapAround ? comboBoxModel.getSize() - 1 : currentIndex) : currentIndex - 1;
  }
}
