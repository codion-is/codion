/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Specifies a table model supporting selection as well as filtering
 * @param <R> the type representing the rows in this table model
 * @param <C> type type used to identify columns in this table model, Integer for indexed identification for example
 * @param <T> the type representing table columns
 */
public interface FilteredTableModel<R, C, T> extends FilteredModel<R> {

  /**
   * @param listener a listener to be notified each time a refresh is about to start
   */
  void addRefreshStartedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshStartedListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time a refresh has finished, successfully or not
   * @see #refresh()
   */
  void addRefreshDoneListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshDoneListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the model has been sorted
   */
  void addSortListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSortListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the table data changes
   */
  void addTableDataChangedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeTableDataChangedListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the table model is cleared
   */
  void addTableModelClearedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeTableModelClearedListener(EventListener listener);

  /**
   * Adds a listener that is notified each time rows are deleted from the data model.
   * @param listener the listener, the data list contains the fromIndex and toIndex as items at index 0 and 1
   */
  void addRowsDeletedListener(EventDataListener<List<Integer>> listener);

  /**
   * @param listener the listener to remove
   */
  void removeRowsDeletedListener(EventDataListener<List<Integer>> listener);

  /**
   * @return true if an impending selection change should be allowed
   */
  boolean allowSelectionChange();

  /**
   * @return the size of the table model
   */
  int getRowCount();

  /**
   * @param item the item
   * @return the index of the item in the table model
   */
  int indexOf(R item);

  /**
   * @param index the index
   * @return the item at the given index in the table model
   */
  R getItemAt(int index);

  /**
   * Removes the given items from this table model
   * @param items the items to remove from the model
   */
  void removeItems(Collection<R> items);

  /**
   * Removes the given item from this table model
   * @param item the item to remove from the model
   */
  void removeItem(R item);

  /**
   * Removes from this table model the visible element whose index is between index
   * @param index the index of the row to be removed
   * @throws IndexOutOfBoundsException in case the indexe is out of bounds
   */
  void removeItemAt(int index);

  /**
   * Removes from this table model all visible elements whose index is between fromIndex, inclusive and toIndex, exclusive
   * @param fromIndex index of first row to be removed
   * @param toIndex index after last row to be removed
   * @throws IndexOutOfBoundsException in case the indexes are out of bounds
   */
  void removeItems(int fromIndex, int toIndex);

  /**
   * @return the TableColumnModel used by this TableModel
   */
  FilteredTableColumnModel<R, C, T> getColumnModel();

  /**
   * Returns the {@link ColumnSummaryModel} associated with {@code columnIdentifier}
   * @param columnIdentifier the column identifier
   * @return the ColumnSummaryModel for the column identified by the given identifier, null if none is available
   */
  ColumnSummaryModel getColumnSummaryModel(C columnIdentifier);

  /**
   * @param columnIdentifier the identifier of the column for which to retrieve the values
   * @param <T> the value type
   * @return the values (including nulls) of the column identified by {@code columnIdentifier} from the rows in the table model
   */
  <T> Collection<T> getValues(C columnIdentifier);

  /**
   * @param columnIdentifier the identifier of the column for which to retrieve the values
   * @param <T> the value type
   * @return the values (including nulls) of the column identified by {@code columnIdentifier} from the selected rows in the table model
   */
  <T> Collection<T> getSelectedValues(C columnIdentifier);

  /**
   * Returns a RowColumn denoting the row and column index of the first value to fulfill
   * the given search condition when searching towards a higher row index.
   * @param fromRowIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param searchText the text to search for
   * @return the search result coordinate, null if nothing was found
   * @see #isRegularExpressionSearch()
   */
  RowColumn findNext(int fromRowIndex, String searchText);

  /**
   * Returns a RowColumn denoting the row and column index of the first value to fulfill
   * the given search condition when searching towards a lower row index.
   * @param fromRowIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param searchText the text to search for
   * @return the search result coordinate, null if nothing was found
   * @see #isRegularExpressionSearch()
   */
  RowColumn findPrevious(int fromRowIndex, String searchText);

  /**
   * Returns a RowColumn denoting the row and column index of the first value to fulfill
   * the given search condition when searching towards a lower row index.
   * @param fromRowIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param condition the search condition
   * @return the search result coordinate, null if nothing was found
   */
  RowColumn findNext(int fromRowIndex, Predicate<String> condition);

  /**
   * Returns a RowColumn denoting the row and column index of the first value to fulfill
   * the given search condition when searching towards a higher row index.
   * @param fromRowIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param condition the search condition
   * @return the search result coordinate, null if nothing was found
   */
  RowColumn findPrevious(int fromRowIndex, Predicate<String> condition);

  /**
   * @return true if regular expressions should be used when searching this table model
   */
  boolean isRegularExpressionSearch();

  /**
   * Specifies whether or not to use regular expressions when searching this table model
   * @param regularExpressionSearch true if regular expression search should be enabled
   */
  void setRegularExpressionSearch(boolean regularExpressionSearch);

  /**
   * @return true if merge on refresh is enabled
   */
  boolean isMergeOnRefresh();

  /**
   * @param mergeOnRefresh true if merge on refresh should be enabled
   */
  void setMergeOnRefresh(boolean mergeOnRefresh);

  /**
   * Sorts the visible contents according to the  {@link TableSortModel}, keeping the selected items.
   * Calling this method with the sort model disabled has no effect.
   * @see #getSortModel()
   * @see #addSortListener(EventListener)
   * @see TableSortModel#isSortingEnabled
   */
  void sort();

  /**
   * @return the selection model used by this table model
   */
  SelectionModel<R> getSelectionModel();

  /**
   * @return the sorting model
   */
  TableSortModel<R, C> getSortModel();

  /**
   * Refreshes the items in this table model
   */
  void refresh();

  /**
   * Clears all items from this table model
   */
  void clear();
}
