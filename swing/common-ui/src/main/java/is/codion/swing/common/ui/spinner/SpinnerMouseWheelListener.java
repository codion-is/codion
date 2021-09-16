/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.spinner;

import javax.swing.SpinnerModel;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import static java.util.Objects.requireNonNull;

/**
 * A simple mouse wheel listener for JSpinner, moving to the next or previous value on wheel spin.
 */
public final class SpinnerMouseWheelListener implements MouseWheelListener {

  private final SpinnerModel spinnerModel;

  /**
   * Instantiates a new mouse wheel listener
   * @param spinnerModel the spinner model
   */
  public SpinnerMouseWheelListener(final SpinnerModel spinnerModel) {
    this.spinnerModel = requireNonNull(spinnerModel);
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent event) {
    final int wheelRotation = event.getWheelRotation();
    if (wheelRotation != 0) {
      final Object newValue = wheelRotation > 0 ? spinnerModel.getNextValue() : spinnerModel.getPreviousValue();
      if (newValue != null) {
        spinnerModel.setValue(newValue);
      }
    }
  }
}
