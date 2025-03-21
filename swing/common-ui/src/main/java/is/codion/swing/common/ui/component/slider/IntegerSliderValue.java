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

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JSlider;

final class IntegerSliderValue extends AbstractComponentValue<Integer, JSlider> {

	IntegerSliderValue(JSlider slider) {
		super(slider, 0);
		slider.getModel().addChangeListener(e -> notifyListeners());
	}

	@Override
	protected Integer getComponentValue() {
		return component().getValue();
	}

	@Override
	protected void setComponentValue(Integer value) {
		component().setValue(value == null ? 0 : value);
	}
}
