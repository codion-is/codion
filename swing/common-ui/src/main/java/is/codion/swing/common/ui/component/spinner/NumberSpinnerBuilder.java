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
package is.codion.swing.common.ui.component.spinner;

import org.jspecify.annotations.Nullable;

/**
 * A builder for number based JSpinner
 */
public interface NumberSpinnerBuilder<T extends Number> extends SpinnerBuilder<T, NumberSpinnerBuilder<T>> {

	/**
	 * @param minimum the minimum value
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> minimum(@Nullable T minimum);

	/**
	 * @param maximum the maximum value
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> maximum(@Nullable T maximum);

	/**
	 * @param stepSize the step size
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> stepSize(@Nullable T stepSize);

	/**
	 * @param groupingUsed true if number format grouping should be used
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> groupingUsed(boolean groupingUsed);

	/**
	 * @param decimalFormatPattern the decimal format pattern
	 * @return this builder instance
	 */
	NumberSpinnerBuilder<T> decimalFormatPattern(@Nullable String decimalFormatPattern);

	/**
	 * @param commitOnValidEdit true if the spinner should commit on a valid edit
	 * @return this builder instance
	 * @see javax.swing.text.DefaultFormatter#setCommitsOnValidEdit(boolean)
	 */
	NumberSpinnerBuilder<T> commitOnValidEdit(boolean commitOnValidEdit);

	/**
	 * Provides a {@link NumberSpinnerBuilder}
	 */
	interface NumberClassStep {

		/**
		 * @param numberClass the number class
		 * @param <T> the number type
		 * @return a new {@link NumberSpinnerBuilder} instance
		 */
		<T extends Number> NumberSpinnerBuilder<T> numberClass(Class<T> numberClass);
	}

	/**
	 * @return a {@link NumberClassStep}
	 */
	static NumberClassStep builder() {
		return DefaultNumberSpinnerBuilder.NUMBER_CLASS;
	}
}
