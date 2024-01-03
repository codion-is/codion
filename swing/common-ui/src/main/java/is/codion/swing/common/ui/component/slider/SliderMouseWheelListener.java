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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.slider;

import javax.swing.BoundedRangeModel;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import static java.util.Objects.requireNonNull;

/**
 * A mouse wheel listener for JSlider, moving to the next or previous value on wheel spin.
 * Up/away increases the value and down/towards decreases it unless reversed.
 */
final class SliderMouseWheelListener implements MouseWheelListener {

  private final BoundedRangeModel boundedRangeModel;
  private final boolean reversed;

  /**
   * Instantiates a new mouse wheel listener
   * @param boundedRangeModel the model
   * @param reversed if true then up/away decreases the value and down/towards increases it.
   */
  SliderMouseWheelListener(BoundedRangeModel boundedRangeModel, boolean reversed) {
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
}
