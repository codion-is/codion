/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.slider;

import javax.swing.BoundedRangeModel;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import static java.util.Objects.requireNonNull;

/**
 * A mouse wheel listener for JSlider, moving to the next or previous value on wheel spin.
 * Up/away increases the value and down/towards decreases it unless reversed.
 * @see #create(BoundedRangeModel)
 * @see #createReversed(BoundedRangeModel)
 */
public final class SliderMouseWheelListener implements MouseWheelListener {

  private final BoundedRangeModel boundedRangeModel;
  private final boolean reversed;

  /**
   * Instantiates a new mouse wheel listener
   * @param boundedRangeModel the model
   * @param reversed if true then up/away decreases the value and down/towards increases it.
   */
  private SliderMouseWheelListener(BoundedRangeModel boundedRangeModel, boolean reversed) {
    this.boundedRangeModel = requireNonNull(boundedRangeModel);
    this.reversed = reversed;
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent event) {
    int wheelRotation = event.getWheelRotation();
    if (wheelRotation != 0) {
      boundedRangeModel.setValue(boundedRangeModel.getValue() + (wheelRotation * (reversed ? 1 : -1)));
    }
  }

  /**
   * Instantiates a new mouse wheel listener
   * @param boundedRangeModel the model
   * @return a new MouseWheelListener
   */
  public static MouseWheelListener create(BoundedRangeModel boundedRangeModel) {
    return new SliderMouseWheelListener(boundedRangeModel, false);
  }

  /**
   * Instantiates a new reversed mouse wheel listener
   * @param boundedRangeModel the model
   * @return a new MouseWheelListener
   */
  public static MouseWheelListener createReversed(BoundedRangeModel boundedRangeModel) {
    return new SliderMouseWheelListener(boundedRangeModel, true);
  }
}
