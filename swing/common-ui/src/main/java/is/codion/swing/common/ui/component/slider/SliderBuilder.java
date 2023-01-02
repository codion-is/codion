/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.slider;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentBuilder;

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
   */
  SliderBuilder minorTickSpacing(int minorTickSpacing);

  /**
   * @param majorTickSpacing the major tick spacing
   * @return this builder instance
   */
  SliderBuilder majorTickSpacing(int majorTickSpacing);

  /**
   * @param snapToTicks snap to ticks
   * @return this builder instance
   */
  SliderBuilder snapToTicks(boolean snapToTicks);

  /**
   * @param paintTicks paint ticks
   * @return this builder instance
   */
  SliderBuilder paintTicks(boolean paintTicks);

  /**
   * @param paintTrack paint track
   * @return this builder instance
   */
  SliderBuilder paintTrack(boolean paintTrack);

  /**
   * @param paintLabels paint labels
   * @return this builder instance
   */
  SliderBuilder paintLabels(boolean paintLabels);

  /**
   * @param inverted should the track be inverted
   * @return this builder instance
   */
  SliderBuilder inverted(boolean inverted);

  /**
   * @param orientation the orientation, SwingConstants.HORIZONTAL or SwingConstants.VERTICAL
   * @return this builder instance
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
