/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JSlider;

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
}
