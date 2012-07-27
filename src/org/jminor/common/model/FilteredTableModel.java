/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.Point;
import java.util.Collection;
import java.util.List;

/**
 * Specifies a table model supporting selection as well as filtering
 * @param <R> the type representing the rows in this table model
 * @param <C> type type used to identify columns in this table model, Integer for simple indexed identification for example
 */
public interface FilteredTableModel<R, C> extends FilteredModel<R>, TableModel, Refreshable {

  /**
   * @return a StateObserver indicating that the selection is empty
   */
  StateObserver getSelectionEmptyObserver();

  /**
   * @return a StateObserver indicating that multiple rows are selected
   */
  StateObserver getMultipleSelectionObserver();

  /**
   * @return a StateObserver indicating that a single row is selected
   */
  StateObserver getSingleSelectionObserver();

  /**
   * @param listener a listener to be notified each time the selection changes
   */
  void addSelectionChangedListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectionChangedListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the selected index changes
   */
  void addSelectedIndexListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedIndexListener(final EventListener listener);

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
   * @param listener a listener to be notified each time a column is hidden
   */
  void addColumnHiddenListener(final EventListener<C> listener);

  /**
   * @param listener the listener to remove
   */
  void removeColumnHiddenListener(final EventListener<C> listener);

  /**
   * @param listener a listener to be notified each time a column is shown
   */
  void addColumnShownListener(final EventListener<C> listener);

  /**
   * @param listener the listener to remove
   */
  void removeColumnShownListener(final EventListener<C> listener);

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
   * Returns the item found at the given index
   * @param index the index
   * @return the item at the given row index
   */
  R getItemAt(final int index);

  /**
   * Returns the index of the given item
   * @param item the item
   * @return the index of the given item or -1 if it was not found
   */
  int indexOf(final R item);

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
   * Removes from this table model all of the elements whose index is between fromIndex, inclusive and toIndex, exclusive
   * @param fromIndex index of first row to be removed
   * @param toIndex index after last row to be removed
   */
  void removeItems(final int fromIndex, final int toIndex);

  /**
   * @return the TableColumnModel used by this TableModel
   */
  TableColumnModel getColumnModel();

  /**
   * Returns the TableColumn with the given identifier
   * @param identifier the column identifier
   * @return the TableColumn with the given identifier, null if none is found
   */
  TableColumn getTableColumn(final C identifier);

  /**
   * @param modelColumnIndex the column model index
   * @return the column identifier
   */
  C getColumnIdentifier(final int modelColumnIndex);

  /**
   * @return an unmodifiable view of hidden table columns
   */
  Collection<TableColumn> getHiddenColumns();

  /**
   * @param columnIdentifier the key for which to query if its column is visible
   * @return true if the column is visible, false if it is hidden
   */
  boolean isColumnVisible(final C columnIdentifier);

  /**
   * Toggles the visibility of the column representing the given columnIdentifier.<br>
   * @param columnIdentifier the column identifier
   * @param visible if true the column is shown, otherwise it is hidden
   */
  void setColumnVisible(final C columnIdentifier, final boolean visible);

  /**
   * @param columnIdentifier the identifier of the column to sort by
   * @param directive the sorting directive
   * @param addColumnToSort if false then the sorting state is cleared, otherwise
   * this column is added to the sorted column set according to <code>getSortingPriority()</code>
   * @see #getSortingPriority(Object)
   */
  void setSortingDirective(final C columnIdentifier, final SortingDirective directive,
                           final boolean addColumnToSort);

  /**
   * @param columnIdentifier the column identifier
   * @return the sorting directive assigned to the given column
   * @throws IllegalArgumentException in case no sorting directive has been set for the given column
   */
  SortingDirective getSortingDirective(final C columnIdentifier);

  /**
   * @param columnIdentifier the column identifier
   * @return the sorting priority for the given column
   * @throws IllegalArgumentException in case no sorting directive has been set for the given column
   */
  int getSortingPriority(final C columnIdentifier);

  /**
   * Returns a Point denoting the row (point.y) and column index (point.x) of the first value to fulfill
   * the given search criteria.
   * @param fromIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param forward if true then the search is forward (towards higher row indexes), backwards otherwise
   * @param searchText the text to search by
   * @return the search result coordinate, null if nothing was found
   * @see #isRegularExpressionSearch()
   * @see FilterCriteria#include(Object)
   */
  Point findNextItemCoordinate(final int fromIndex, final boolean forward, final String searchText);

  /**
   * Returns a Point denoting the row (point.y) and column index (point.x) of the first value to fulfill
   * the given search criteria.
   * @param fromIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param forward if true then the search is forward (towards higher row indexes), backwards otherwise
   * @param criteria the search criteria
   * @return the search result coordinate, null if nothing was found
   * @see FilterCriteria#include(Object)
   */
  Point findNextItemCoordinate(final int fromIndex, final boolean forward, final FilterCriteria<Object> criteria);

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
   * @param columnIdentifier the column identifier
   * @return the SearchModel at the given column index
   */
  ColumnSearchModel<C> getFilterModel(final C columnIdentifier);

  /**
   * @return true if no rows are selected in this table model
   */
  boolean isSelectionEmpty();

  /**
   * @return the index of the selected record, -1 if none is selected and
   * the lowest index if more than one record is selected
   */
  int getSelectedIndex();

  /**
   * Selects the item at <code>index</code>
   * @param index the index
   */
  void addSelectedItemIndex(final int index);

  /**
   * Clears the selection and selects the item at <code>index</code>
   * @param index the index
   */
  void setSelectedItemIndex(final int index);

  /**
   * Clears the selection
   * @see #addSelectionChangedListener(EventListener)
   */
  void clearSelection();

  /**
   * Selects the given indexes
   * @param indexes the indexes to select
   */
  void setSelectedItemIndexes(final Collection<Integer> indexes);

  /**
   * Selects the given items
   * @param items the items to select
   */
  void setSelectedItems(final Collection<R> items);

  /**
   * @return a list containing the selected items
   */
  List<R> getSelectedItems();

  /**
   * @return the selected item, null if none is selected
   */
  R getSelectedItem();

  /**
   * @return the selected indexes, an empty list if selection is empty
   */
  List<Integer> getSelectedIndexes();

  /**
   * Sets the selected item
   * @param item the item to select
   */
  void setSelectedItem(final R item);

  /**
   * Adds the given item to the selection
   * @param item the item to add to the selection
   */
  void addSelectedItem(final R item);

  /**
   * Adds the given items to the selection
   * @param items the items to add to the selection
   */
  void addSelectedItems(final Collection<R> items);

  /**
   * Selects all visible entities
   * @see #addSelectionChangedListener(EventListener)
   */
  void selectAll();

  /**
   * Adds these indexes to the selection
   * @param indexes the indexes to add to the selection
   */
  void addSelectedItemIndexes(final Collection<Integer> indexes);

  /**
   * @return the number of selected indexes in the underlying selection model.
   */
  int getSelectionCount();

  /**
   * Moves all selected indexes down one index, wraps around
   * @see #addSelectionChangedListener(EventListener)
   */
  void moveSelectionDown();

  /**
   * Moves all selected indexes up one index, wraps around
   * @see #addSelectionChangedListener(EventListener)
   */
  void moveSelectionUp();

  /**
   * @return the selection model used by this table model
   */
  ListSelectionModel getSelectionModel();

  /**
   * Specifies a sorting state for a column.
   */
  interface SortingState {

    /**
     * @return the sorting directive currently associated with the column
     */
    SortingDirective getDirective();

    /**
     * @return the sorting priority, 0 being the lowest
     */
    int getPriority();
  }
}
