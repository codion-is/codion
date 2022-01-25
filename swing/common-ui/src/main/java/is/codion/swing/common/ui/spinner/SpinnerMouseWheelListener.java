/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.spinner;

import javax.swing.SpinnerModel;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import static java.util.Objects.requireNonNull;

/**
 * A simple mouse wheel listener for JSpinner, moving to the next or previous value on wheel spin.
 * Up/away increases the value and down/towards decreases it unless reversed.
 * @see #create(SpinnerModel)
 * @see #createReversed(SpinnerModel)
 */
public final class SpinnerMouseWheelListener implements MouseWheelListener {

  private final SpinnerModel spinnerModel;
  private final boolean reversed;

  /**
   * Instantiates a new mouse wheel listener
   * @param spinnerModel the spinner model
   * @param reversed if true then up/away decreases the value and down/towards increases it.
   */
  private SpinnerMouseWheelListener(final SpinnerModel spinnerModel, final boolean reversed) {
    this.spinnerModel = requireNonNull(spinnerModel);
    this.reversed = reversed;
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent event) {
    final int wheelRotation = event.getWheelRotation();
    if (wheelRotation != 0) {
      final Object newValue = (reversed ? wheelRotation > 0 : wheelRotation < 0) ? spinnerModel.getNextValue() : spinnerModel.getPreviousValue();
      if (newValue != null) {
        spinnerModel.setValue(newValue);
      }
    }
  }

  /**
   * Instantiates a new mouse wheel listener
   * @param spinnerModel the spinner model
   * @return a new MouseWheelListener
   */
  public static MouseWheelListener create(final SpinnerModel spinnerModel) {
    return new SpinnerMouseWheelListener(spinnerModel, false);
  }

  /**
   * Instantiates a new reversed mouse wheel listener
   * @param spinnerModel the spinner model
   * @return a new MouseWheelListener
   */
  public static MouseWheelListener createReversed(final SpinnerModel spinnerModel) {
    return new SpinnerMouseWheelListener(spinnerModel, true);
  }
}
