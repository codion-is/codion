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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Handles searching through a {@link FilteredTable}
 */
public interface FilteredTableSearchModel {

	/**
	 * @return the state controlling whether regular expressions should be used when searching
	 */
	State regularExpression();

	/**
	 * @return the state controlling whether searching is case-sensitive
	 */
	State caseSensitive();

	/**
	 * @return the Value for the search string
	 */
	Value<String> searchString();

	/**
	 * @return the value for the search predicate
	 */
	Value<Predicate<String>> searchPredicate();

	/**
	 * Finds the next value and selects the row, if none is found the selection is cleared
	 * @return the row and column of the next item fitting the search condition, an empty Optional if none is found
	 */
	Optional<RowColumn> nextResult();

	/**
	 * Finds the next value and adds the row to the selection
	 * @return the row and column of the next item fitting the search condition, an empty Optional if none is found
	 */
	Optional<RowColumn> selectNextResult();

	/**
	 * Finds the previous value and selects the row, if none is found the selection is cleared
	 * @return the row and column of the previous item fitting the search condition, an empty Optional if none is found
	 */
	Optional<RowColumn> previousResult();

	/**
	 * Finds the previous value and adds the row to the selection
	 * @return the row and column of the previous item fitting the search condition, an empty Optional if none is found
	 */
	Optional<RowColumn> selectPreviousResult();

	/**
	 * @return an unmodifiable view of all row/column search results
	 */
	List<RowColumn> searchResults();

	/**
	 * Returns the Value holding the selected search result row/column if available, otherwise one with row: -1 and column: -1
	 * @return an observer notified each time the current search result changes
	 * @see #nextResult()
	 * @see #previousResult()
	 */
	ValueObserver<RowColumn> currentResult();

	/**
	 * Holds a row/column coordinate
	 */
	interface RowColumn {

		/**
		 * @return the row
		 */
		int row();

		/**
		 * @return the column
		 */
		int column();

		/**
		 * @param row the row
		 * @param column the column
		 * @return true if this RowColumn instance represents the given row and column
		 */
		default boolean equals(int row, int column) {
			return row() == row && column == column();
		}
	}
}
