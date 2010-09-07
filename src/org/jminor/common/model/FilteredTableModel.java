/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

/**
 * Specifies a table model that can be filtered.
 * @param <T> the type of items in the table model
 * @param <C> the type of column identifiers used by the table model
 */
public interface FilteredTableModel<T, C> extends FilteredModel<T>, TableModel, Refreshable {

  /**
   * @return a State active when the selection is empty
   */
  StateObserver getSelectionEmptyState();

  /**
   * @return a State active when multiple rows are selected
   */
  StateObserver getMultipleSelectionState();

  /**
   * @param listener a listener to be notified each time the selection changes
   */
  void addSelectionChangedListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectionChangedListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time the selected index changes
   */
  void addSelectedIndexListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedIndexListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time a refresh is about to start
   */
  void addRefreshStartedListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshStartedListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time a refresh has ended
   */
  void addRefreshDoneListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshDoneListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time the model has been sorted or the sorting state has been cleared
   */
  void addSortingListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSortingListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time a column is hidden
   */
  void addColumnHiddenListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeColumnHiddenListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time a column is shown
   */
  void addColumnShownListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeColumnShownListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time the table data changes
   */
  void addTableDataChangedListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeTableDataChangedListener(final ActionListener listener);

  /**
   * Returns the item found at the given index
   * @param index the index
   * @return the item at the given row index
   */
  T getItemAt(final int index);

  /**
   * Returns the index of the given item
   * @param item the item
   * @return the index of the given item or -1 if it was not found
   */
  int indexOf(final T item);

  /**
   * Removes the given items from this table model
   * @param items the items to remove from the model
   */
  void removeItems(final List<T> items);

  /**
   * Removes the given item from this table model
   * @param item the item to remove from the model
   */
  void removeItem(final T item);

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
   */
  void setSortingDirective(final C columnIdentifier, final SortingDirective directive);

  /**
   * @param columnIdentifier the column identifier
   * @return the sorting directive assigned to the given column
   */
  SortingDirective getSortingDirective(final C columnIdentifier);

  /**
   * @param columnIdentifier the column identifier
   * @return the sorting priority for the given column
   */
  int getSortingPriority(final C columnIdentifier);

  /**
   * Returns a Point denoting the row (point.y) and column index (point.x) of the first value to fulfill
   * the given search criteria.
   * @param fromIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param forward if true then the search is forward, backwards otherwise
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
   * @param forward if true then the search is forward, backwards otherwise
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
   * @see #addSelectionChangedListener(java.awt.event.ActionListener)
   */
  void clearSelection();

  /**
   * Selects the given indexes
   * @param indexes the indexes to select
   */
  void setSelectedItemIndexes(final List<Integer> indexes);

  /**
   * Selects the given items
   * @param items the items to select
   */
  void setSelectedItems(final List<T> items);

  /**
   * @return a list containing the selected items
   */
  List<T> getSelectedItems();

  /**
   * @return the selected item, null if none is selected
   */
  T getSelectedItem();

  /**
   * @return the selected indexes, an empty collection if selection is empty
   */
  Collection<Integer> getSelectedIndexes();

  /**
   * Sets the selected item
   * @param item the item to select
   */
  void setSelectedItem(final T item);

  /**
   * Selects all visible entities
   * @see #addSelectionChangedListener(java.awt.event.ActionListener)
   */
  void selectAll();

  /**
   * Adds these indexes to the selection
   * @param indexes the indexes to add to the selection
   */
  void addSelectedItemIndexes(final List<Integer> indexes);

  /**
   * @return the number of selected indexes in the underlying selection model.
   */
  int getSelectionCount();

  /**
   * Moves all selected indexes down one index, wraps around
   * @see #addSelectionChangedListener(java.awt.event.ActionListener)
   */
  void moveSelectionDown();

  /**
   * Moves all selected indexes up one index, wraps around
   * @see #addSelectionChangedListener(java.awt.event.ActionListener)
   */
  void moveSelectionUp();

  /**
   * @return the selection model used by this table model
   */
  ListSelectionModel getSelectionModel();

  /**
   * Clears the column sorting states, without reordering the rows
   */
  void clearSortingState();

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
