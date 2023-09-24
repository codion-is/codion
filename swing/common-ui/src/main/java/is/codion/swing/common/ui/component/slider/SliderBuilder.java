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
import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;

import static java.util.Objects.requireNonNull;

/**
 * A builder for JSpinner
 */
public interface SliderBuilder extends ComponentBuilder<Integer, JSlider, SliderBuilder> {

  /**
   * @param minorTickSpacing the minor tick spacing
   * @return this builder instance
   * @see JSlider#setMinorTickSpacing(int)
   */
  SliderBuilder minorTickSpacing(int minorTickSpacing);

  /**
   * @param majorTickSpacing the major tick spacing
   * @return this builder instance
   * @see JSlider#setMajorTickSpacing(int)
   */
  SliderBuilder majorTickSpacing(int majorTickSpacing);

  /**
   * @param snapToTicks snap to ticks
   * @return this builder instance
   * @see JSlider#setSnapToTicks(boolean)
   */
  SliderBuilder snapToTicks(boolean snapToTicks);

  /**
   * @param paintTicks paint ticks
   * @return this builder instance
   * @see JSlider#setPaintTicks(boolean)
   */
  SliderBuilder paintTicks(boolean paintTicks);

  /**
   * @param paintTrack paint track
   * @return this builder instance
   * @see JSlider#setPaintTrack(boolean)
   */
  SliderBuilder paintTrack(boolean paintTrack);

  /**
   * @param paintLabels paint labels
   * @return this builder instance
   * @see JSlider#setPaintLabels(boolean)
   */
  SliderBuilder paintLabels(boolean paintLabels);

  /**
   * @param inverted should the track be inverted
   * @return this builder instance
   * @see JSlider#setInverted(boolean)
   */
  SliderBuilder inverted(boolean inverted);

  /**
   * @param orientation the orientation, SwingConstants.HORIZONTAL or SwingConstants.VERTICAL
   * @return this builder instance
   * @see JSlider#setOrientation(int)
   */
  SliderBuilder orientation(int orientation);

  /**
   * Enable mouse wheel scrolling on the slider
   * @param mouseWheelScrolling true if mouse wheel scrolling should be enabled
   * @return this builder instance
   */
  SliderBuilder mouseWheelScrolling(boolean mouseWheelScrolling);

  /**
   * Enable reversed mouse wheel scrolling on the slider
   * @param mouseWheelScrollingReversed if true then up/away decreases the value and down/towards increases it.
   * @return this builder instance
   */
  SliderBuilder mouseWheelScrollingReversed(boolean mouseWheelScrollingReversed);

  /**
   * @param boundedRangeModel the slider model
   * @return a builder for a component
   */
  static SliderBuilder builder(BoundedRangeModel boundedRangeModel) {
    return new DefaultSliderBuilder(boundedRangeModel, null);
  }

  /**
   * @param boundedRangeModel the slider model
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  static SliderBuilder builder(BoundedRangeModel boundedRangeModel, Value<Integer> linkedValue) {
    return new DefaultSliderBuilder(boundedRangeModel, requireNonNull(linkedValue));
  }
}
