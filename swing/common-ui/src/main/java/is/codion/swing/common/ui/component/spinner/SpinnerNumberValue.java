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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

final class SpinnerNumberValue<T extends Number> extends AbstractComponentValue<T, JSpinner> {

	SpinnerNumberValue(JSpinner spinner, Class<T> valueClass) {
		super(spinner, nullValue((SpinnerNumberModel) spinner.getModel(), valueClass));
		set(null);
		addValidator(new SpinnerModelValidator<>((SpinnerNumberModel) component().getModel()));
		spinner.getModel().addChangeListener(e -> notifyListeners());
	}

	@Override
	protected T getComponentValue() {
		return (T) component().getValue();
	}

	@Override
	protected void setComponentValue(T value) {
		component().setValue(value);
	}

	private static <T extends Number> T nullValue(SpinnerNumberModel model, Class<T> valueClass) {
		Comparable<T> minimumValue = (Comparable<T>) model.getMinimum();
		Comparable<T> maximumValue = (Comparable<T>) model.getMaximum();
		if (minimumValue != null) {
			return (T) minimumValue;
		}
		if (maximumValue != null) {
			return (T) maximumValue;
		}
		if (valueClass.equals(Integer.class)) {
			return (T) Integer.valueOf(0);
		}
		if (valueClass.equals(Double.class)) {
			return (T) Double.valueOf(0);
		}

		throw new IllegalArgumentException("Cannot create null value for valueClass: " + valueClass);
	}

	private static final class SpinnerModelValidator<T> implements Validator<T> {

		private final SpinnerNumberModel model;

		private SpinnerModelValidator(SpinnerNumberModel model) {
			this.model = model;
		}

		@Override
		public void validate(T value) {
			Comparable<T> minimumValue = (Comparable<T>) model.getMinimum();
			if (value != null && minimumValue != null && minimumValue.compareTo(value) > 0) {
				throw new IllegalArgumentException("Value must be greater than or equal to the minimum value " + minimumValue);
			}
			Comparable<T> maximumValue = (Comparable<T>) model.getMaximum();
			if (value != null && maximumValue != null && maximumValue.compareTo(value) < 0) {
				throw new IllegalArgumentException("Value must be less than or equal to the maximum value " + maximumValue);
			}
		}
	}
}
