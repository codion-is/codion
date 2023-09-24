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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.slider;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

final class DefaultSliderBuilder extends AbstractComponentBuilder<Integer, JSlider, SliderBuilder> implements SliderBuilder {

  private final BoundedRangeModel boundedRangeModel;

  private int minorTickSpacing;
  private int majorTickSpacing;
  private boolean snapToTicks = false;
  private boolean paintTicks = false;
  private boolean paintTrack = true;
  private boolean paintLabels = false;
  private boolean inverted = false;
  private int orientation = SwingConstants.HORIZONTAL;
  private boolean mouseWheelScrolling = true;
  private boolean mouseWheelScrollingReversed = false;

  DefaultSliderBuilder(BoundedRangeModel boundedRangeModel, Value<Integer> linkedValue) {
    super(linkedValue);
    this.boundedRangeModel = requireNonNull(boundedRangeModel);
  }

  @Override
  public SliderBuilder minorTickSpacing(int minorTickSpacing) {
    this.minorTickSpacing = minorTickSpacing;
    return this;
  }

  @Override
  public SliderBuilder majorTickSpacing(int majorTickSpacing) {
    this.majorTickSpacing = majorTickSpacing;
    return this;
  }

  @Override
  public SliderBuilder snapToTicks(boolean snapToTicks) {
    this.snapToTicks = snapToTicks;
    return this;
  }

  @Override
  public SliderBuilder paintTicks(boolean paintTicks) {
    this.paintTicks = paintTicks;
    return this;
  }

  @Override
  public SliderBuilder paintTrack(boolean paintTrack) {
    this.paintTrack = paintTrack;
    return this;
  }

  @Override
  public SliderBuilder paintLabels(boolean paintLabels) {
    this.paintLabels = paintLabels;
    return this;
  }

  @Override
  public SliderBuilder inverted(boolean inverted) {
    this.inverted = inverted;
    return this;
  }

  @Override
  public SliderBuilder orientation(int orientation) {
    this.orientation = orientation;
    return this;
  }

  @Override
  public SliderBuilder mouseWheelScrolling(boolean mouseWheelScrolling) {
    this.mouseWheelScrolling = mouseWheelScrolling;
    if (mouseWheelScrolling) {
      this.mouseWheelScrollingReversed = false;
    }
    return this;
  }

  @Override
  public SliderBuilder mouseWheelScrollingReversed(boolean mouseWheelScrollingReversed) {
    this.mouseWheelScrollingReversed = mouseWheelScrollingReversed;
    if (mouseWheelScrollingReversed) {
      this.mouseWheelScrolling = false;
    }
    return this;
  }

  @Override
  protected JSlider createComponent() {
    JSlider slider = new JSlider(boundedRangeModel);
    if (minorTickSpacing > 0) {
      slider.setMinorTickSpacing(minorTickSpacing);
    }
    if (majorTickSpacing > 0) {
      slider.setMajorTickSpacing(majorTickSpacing);
    }
    slider.setSnapToTicks(snapToTicks);
    slider.setPaintTicks(paintTicks);
    slider.setPaintTrack(paintTrack);
    slider.setPaintLabels(paintLabels);
    slider.setInverted(inverted);
    slider.setOrientation(orientation);
    if (mouseWheelScrolling) {
      slider.addMouseWheelListener(new SliderMouseWheelListener(boundedRangeModel, false));
    }
    if (mouseWheelScrollingReversed) {
      slider.addMouseWheelListener(new SliderMouseWheelListener(boundedRangeModel, true));
    }

    return slider;
  }

  @Override
  protected ComponentValue<Integer, JSlider> createComponentValue(JSlider component) {
    return new IntegerSliderValue(component);
  }

  @Override
  protected void setInitialValue(JSlider component, Integer initialValue) {
    component.setValue(initialValue);
  }
}
