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
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;

import javax.swing.SpinnerNumberModel;

import static java.util.Objects.requireNonNull;

/**
 * A builder for number based JSpinner
 */
public interface NumberSpinnerBuilder<T extends Number> extends SpinnerBuilder<T, NumberSpinnerBuilder<T>> {

	/**
	 * @param minimum the minimum value
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> minimum(T minimum);

	/**
	 * @param maximum the maximum value
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> maximum(T maximum);

	/**
	 * @param stepSize the step size
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> stepSize(T stepSize);

	/**
	 * @param groupingUsed true if number format grouping should be used
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> groupingUsed(boolean groupingUsed);

	/**
	 * @param decimalFormatPattern the decimal format pattern
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> decimalFormatPattern(String decimalFormatPattern);

	/**
	 * @param commitOnValidEdit true if the spinner should commit on a valid edit
	 * @return this builder instance
	 * @see javax.swing.text.DefaultFormatter#setCommitsOnValidEdit(boolean)
	 */
	NumberSpinnerBuilder<T> commitOnValidEdit(boolean commitOnValidEdit);

	/**
	 * @param spinnerNumberModel the spinner model
	 * @param valueClass the value class
	 * @param <T> the number type
	 * @return a new {@link NumberSpinnerBuilder} instance
	 */
	static <T extends Number> NumberSpinnerBuilder<T> builder(SpinnerNumberModel spinnerNumberModel,
																														Class<T> valueClass) {
		return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, valueClass, null);
	}

	/**
	 * @param spinnerNumberModel the spinner model
	 * @param valueClass the value class
	 * @param linkedValue the value to link to
	 * @param <T> the number type
	 * @return a new {@link NumberSpinnerBuilder} instance
	 */
	static <T extends Number> NumberSpinnerBuilder<T> builder(SpinnerNumberModel spinnerNumberModel,
																														Class<T> valueClass, Value<T> linkedValue) {
		return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, valueClass, requireNonNull(linkedValue));
	}
}
