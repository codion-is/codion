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
package is.codion.common.model.table;

import is.codion.common.event.EventObserver;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A interface defining a class for providing summaries of numerical table columns: sum, average, minimum, maximum and minimum &#38; maximum.<br>
 * For instances use the {@link #columnSummaryModel(SummaryValues)} factory method.
 * @see #columnSummaryModel(SummaryValues)
 */
public interface ColumnSummaryModel {

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
	 * @return the value controlling the summary
	 */
	Value<Summary> summary();

	/**
	 * @return an observer for the string representing the summary value
	 */
	ValueObserver<String> summaryText();

	/**
	 * Instantiates a new {@link ColumnSummaryModel}
	 * @param summaryValues the summary values
	 * @param <T> the value type
	 * @return a new {@link ColumnSummaryModel} instance
	 */
	static <T extends Number> ColumnSummaryModel columnSummaryModel(SummaryValues<T> summaryValues) {
		return new DefaultColumnSummaryModel<>(summaryValues);
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
		 * @return an observer notified when underlying data changes, requiring a summary refresh
		 */
		EventObserver<?> changeEvent();

		/**
		 * @param <C> the column identifier type
		 */
		interface Factory<C> {

			/**
			 * @param columnIdentifier the column identifier
			 * @param format the format to use
			 * @param <T> the column type
			 * @return a summary values instance or an empty Optional, if no summary is available for the column
			 */
			<T extends Number> Optional<SummaryValues<T>> createSummaryValues(C columnIdentifier, Format format);
		}
	}
}
