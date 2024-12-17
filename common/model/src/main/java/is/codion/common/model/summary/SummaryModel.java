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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.summary;

import is.codion.common.observable.Observable;
import is.codion.common.observable.Observer;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A interface defining a class for providing summaries of numerical table columns: sum, average, minimum, maximum and minimum &#38; maximum.
 * <p>
 * For instances use the {@link #summaryModel(SummaryValues)} factory method.
 * @see #summaryModel(SummaryValues)
 */
public interface SummaryModel {

	/**
	 * Specifies a summary provider
	 */
	interface Summary {

		/**
		 * Returns a String containing the summary information for the given column
		 * @param summaryValues the object responsible for providing the values for the summary
		 * @param <T> the value type
		 * @return a summary text
		 */
		<T extends Number> String summary(SummaryValues<T> summaryValues);
	}

	/**
	 * @return the locked state, if true then changing summary type is disabled
	 */
	State locked();

	/**
	 * @return a list containing the available summaries
	 */
	List<Summary> summaries();

	/**
	 * @return the {@link Value} controlling the summary
	 */
	Value<Summary> summary();

	/**
	 * @return an observable for the string representing the summary value
	 */
	Observable<String> summaryText();

	/**
	 * Instantiates a new {@link SummaryModel}
	 * @param summaryValues the summary values
	 * @param <T> the value type
	 * @return a new {@link SummaryModel} instance
	 */
	static <T extends Number> SummaryModel summaryModel(SummaryValues<T> summaryValues) {
		return new DefaultSummaryModel<>(summaryValues);
	}

	/**
	 * Provides the values on which to base the summary .
	 * @param <T> the value type
	 */
	interface SummaryValues<T extends Number> {

		/**
		 * @param value the value
		 * @return the formatted value
		 */
		String format(Object value);

		/**
		 * @return the values to base the summary on
		 */
		Collection<T> values();

		/**
		 * @return true if the values provided by {@link #values()} is a subset of the total available values
		 */
		boolean subset();

		/**
		 * @return an observer notified when underlying values change, requiring a summary refresh
		 */
		Observer<?> valuesChanged();

		/**
		 * @param <C> the column identifier type
		 */
		interface Factory<C> {

			/**
			 * @param identifier the column identifier
			 * @param format the format to use
			 * @param <T> the column type
			 * @return a summary values instance or an empty Optional, if no summary is available for the column
			 */
			<T extends Number> Optional<SummaryValues<T>> createSummaryValues(C identifier, Format format);
		}
	}
}
