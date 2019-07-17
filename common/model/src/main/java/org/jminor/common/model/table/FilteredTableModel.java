/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.EventListener;
import org.jminor.common.model.FilterCondition;
import org.jminor.common.model.FilteredModel;
import org.jminor.common.model.Refreshable;

import java.util.Collection;

/**
 * Specifies a table model supporting selection as well as filtering
 * @param <R> the type representing the rows in this table model
 * @param <C> type type used to identify columns in this table model, Integer for simple indexed identification for example
 * @param <T> the type representing table columns
 */
public interface FilteredTableModel<R, C, T> extends FilteredModel<R>, TableModelProxy<R>, Refreshable {

  /**
   * Enum indicating how to add items to a FilteredTableModel
   */
  enum AddingStrategy {
    /**
     * Add at the top
     */
    TOP,
    /**
     * Add at the bottom
     */
    BOTTOM,
    /**
     * Add at the top then sort if sorting is enabled
     */
    TOP_SORTED
  }

  /**
   * @param listener a listener to be notified each time a refresh is about to start
   */
  void addRefreshStartedListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshStartedListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time a refresh has ended
   */
  void addRefreshDoneListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshDoneListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the model has been sorted or the sorting state has been cleared
   */
  void addSortingListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSortingListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the table data changes
   */
  void addTableDataChangedListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeTableDataChangedListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the table model is cleared
   */
  void addTableModelClearedListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeTableModelClearedListener(final EventListener listener);

  /**
   * Removes the given items from this table model
   * @param items the items to remove from the model
   */
  void removeItems(final Collection<R> items);

  /**
   * Removes the given item from this table model
   * @param item the item to remove from the model
   */
  void removeItem(final R item);

  /**
   * Removes from this table model all visible elements whose index is between fromIndex, inclusive and toIndex, exclusive
   * @param fromIndex index of first row to be removed
   * @param toIndex index after last row to be removed
   * @throws IndexOutOfBoundsException in case the indexes are out of bounds
   */
  void removeItems(final int fromIndex, final int toIndex);

  /**
   * @return the TableColumnModel used by this TableModel
   */
  FilteredTableColumnModel<C, T> getColumnModel();

  /**
   * @param columnIdentifier the column identifier
   * @return the ColumnSummaryModel for the column identified by the given
   */
  ColumnSummaryModel getColumnSummaryModel(final C columnIdentifier);

  /**
   * @param columnIdentifier the identifier of the column for which to retrieve the values
   * @param selectedOnly if true only values from the selected rows are returned
   * @return the values (including nulls) of the column identified by {@code columnIdentifier} from the rows in the table model
   */
  Collection getValues(final C columnIdentifier, final boolean selectedOnly);

  /**
   * Returns a RowColumn denoting the row and column index of the first value to fulfill
   * the given search condition.
   * @param fromIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param forward if true then the search is forward (towards higher row indexes), backwards otherwise
   * @param searchText the text to search by
   * @return the search result coordinate, null if nothing was found
   * @see #isRegularExpressionSearch()
   * @see FilterCondition#include(Object)
   */
  RowColumn findNextItemCoordinate(final int fromIndex, final boolean forward, final String searchText);

  /**
   * Returns a RowColumn denoting the row and column index of the first value to fulfill
   * the given search condition.
   * @param fromIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param forward if true then the search is forward (towards higher row indexes), backwards otherwise
   * @param condition the search condition
   * @return the search result coordinate, null if nothing was found
   * @see FilterCondition#include(Object)
   */
  RowColumn findNextItemCoordinate(final int fromIndex, final boolean forward, final FilterCondition<Object> condition);

  /**
   * @return true if regular expressions should be used when searching this table model
   */
  boolean isRegularExpressionSearch();

  /**
   * Specifies whether or not to use regular expressions when searching this table model
   * @param value the value
   */
  void setRegularExpressionSearch(final boolean value);

  /**
   * Sorts the visible contents according to the {@link TableSortModel}, keeping the selection state intact
   * @see #getSortModel()
   */
  void sortContents();

  /**
   * @return the selection model used by this table model
   */
  SelectionModel<R> getSelectionModel();

  /**
   * @return the sorting model
   */
  TableSortModel<R, C, T> getSortModel();
}
