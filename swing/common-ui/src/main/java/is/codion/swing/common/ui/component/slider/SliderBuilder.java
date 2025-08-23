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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.slider;

import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;

import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;

import static is.codion.common.Configuration.booleanValue;

/**
 * A builder for JSpinner
 */
public interface SliderBuilder extends ComponentValueBuilder<JSlider, Integer, SliderBuilder> {

	/**
	 * Specifies whether mouse wheel scrolling in sliders is enabled by default.
	 * <ul>
	 * <li>Value type:Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> MOUSE_WHEEL_SCROLLING =
					booleanValue(SliderBuilder.class.getName() + ".mouseWheelScrolling", true);

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
	 * @see #MOUSE_WHEEL_SCROLLING
	 */
	SliderBuilder mouseWheelScrolling(boolean mouseWheelScrolling);

	/**
	 * Enable reversed mouse wheel scrolling on the slider
	 * @param mouseWheelScrollingReversed if true then up/away decreases the value and down/towards increases it.
	 * @return this builder instance
	 */
	SliderBuilder mouseWheelScrollingReversed(boolean mouseWheelScrollingReversed);

	/**
	 * Provides a {@link SliderBuilder}
	 */
	interface ModelStep {

		/**
		 * @param boundedRangeModel the model
		 * @return a {@link SliderBuilder}
		 */
		SliderBuilder model(BoundedRangeModel boundedRangeModel);
	}

	/**
	 * @return a builder for a component
	 */
	static ModelStep builder() {
		return DefaultSliderBuilder.MODEL;
	}
}
