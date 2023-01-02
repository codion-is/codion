/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Handles searching through a {@link FilteredTableModel}
 */
public interface FilteredTableSearchModel {

  /**
   * @return the state controlling whether regular expressions should be used when searching
   */
  State regularExpressionSearchState();

  /**
   * @return the state controlling whether searching is case-sensitive
   */
  State caseSensitiveSearchState();

  /**
   * @return the Value for the search string
   */
  Value<String> searchStringValue();

  /**
   * @return the value for the search predicate
   */
  Value<Predicate<String>> searchPredicateValue();

  /**
   * Finds the next value and selects the row, if none is found the selection is cleared
   * @return the row and column of the next item fitting the search criteria, an empty Optional if none is found
   */
  Optional<RowColumn> nextResult();

  /**
   * Finds the next value and adds the row to the selection
   * @return the row and column of the next item fitting the search criteria, an empty Optional if none is found
   */
  Optional<RowColumn> selectNextResult();

  /**
   * Finds the previous value and selects the row, if none is found the selection is cleared
   * @return the row and column of the previous item fitting the search criteria, an empty Optional if none is found
   */
  Optional<RowColumn> previousResult();

  /**
   * Finds the previous value and adds the row to the selection
   * @return the row and column of the previous item fitting the search criteria, an empty Optional if none is found
   */
  Optional<RowColumn> selectPreviousResult();

  /**
   * @return an unmodifiable view of all row/column search results
   */
  List<RowColumn> searchResults();

  /**
   * @return the selected search result row/column if available, otherwise one with row: -1 and column: -1
   */
  RowColumn currentResult();

  /**
   * @param listener a listener notified each time the current search result changes
   * @see #nextResult()
   * @see #previousResult()
   */
  void addCurrentResultListener(EventDataListener<RowColumn> listener);

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
  }
}
