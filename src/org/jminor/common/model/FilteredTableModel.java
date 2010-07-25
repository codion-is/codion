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

public interface FilteredTableModel<T, C> extends FilteredModel<T>, TableModel, Refreshable {

  /**
   * @return a State active when the selection is empty
   */
  State stateSelectionEmpty();

  /**
   * @return a State active when multiple rows are selected
   */
  State stateMultipleSelection();

  /**
   * @return an Event fired after the selection has changed
   */
  Event eventSelectionChanged();

  /**
   * @return an event fired when the minimum (topmost) selected index changes (minSelectionIndex property in ListSelectionModel)
   */
  Event eventSelectedIndexChanged();

  /**
   * @return an Event fired when the model is about to be refreshed
   */
  Event eventRefreshStarted();

  /**
   * @return an Event fired when the model has been refreshed, N.B. this event
   * is fired even if the refresh results in an exception
   */
  Event eventRefreshDone();

  /**
   * @return an Event fired when the model is about to be sorted
   */
  Event eventSortingStarted();

  /**
   * @return an Event fired when the model has been sorted
   */
  Event eventSortingDone();

  /**
   * @return an Event fired whenever a column is hidden,
   * the ActionEvent source is the column identifier.
   */
  Event eventColumnHidden();

  /**
   * @return an Event fired whenever a column is shown,
   * the ActionEvent source is the column identifier.
   */
  Event eventColumnShown();

  /**
   * @return an Event fired when the selection is changing
   */
  Event eventSelectionChangedAdjusting();

  /**
   * @return an Event fired after the table data has changed
   */
  Event eventTableDataChanged();

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
  TableColumn getTableColumn(final Object identifier);

  /**
   * @return an unmodifiable view of hidden table columns
   */
  Collection<TableColumn> getHiddenColumns();

  /**
   * Shows the column identified by the given identifier,
   * if the column is visible calling this method has no effect
   * @param columnIdentifier the column identifier
   */
  void showColumn(final Object columnIdentifier);

  /**
   * Hides the column identified by the given identifier,
   * if the column is already hidden calling this method has no effect
   * @param columnIdentifier the column identifier
   */
  void hideColumn(final Object columnIdentifier);

  /**
   * @param columnIdentifier the key for which to query if its column is visible
   * @return true if the column is visible, false if it is hidden
   */
  boolean isColumnVisible(final Object columnIdentifier);

  /**
   * Toggles the visibility of the column representing the given columnIdentifier.<br>
   * @param columnIdentifier the column identifier
   * @param visible if true the column is shown, otherwise it is hidden
   */
  void setColumnVisible(final Object columnIdentifier, final boolean visible);

  /**
   * @param columnIndex the index of the column to sort by
   * @param directive the sorting directive
   */
  void setSortingDirective(final int columnIndex, final SortingDirective directive);

  /**
   * @param columnIndex the column index
   * @return the sorting directive assigned to the given column
   */
  SortingDirective getSortingDirective(final int columnIndex);

  /**
   * @param columnIndex the column index
   * @return the sort priority for the given column
   */
  int getSortPriority(final int columnIndex);

  /**
   * @param objectOne the first item to compare
   * @param objectTwo the second item to compare
   * @param columnIndex the index of the column on which to base the comparison
   * @param directive the sorting directive
   * @return the compare result
   */
  int compare(final T objectOne, final T objectTwo, final int columnIndex, final SortingDirective directive);

  /**
   * Clears all sorting states from the columns in this model
   */
  void clearSortingState();

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
   * @param columnIndex the column index
   * @return the SearchModel at the given column index
   */
  SearchModel<C> getFilterModel(final int columnIndex);

  /**
   * @return an unmodifiable list view of the column filter models
   */
  List<SearchModel<C>> getFilterModels();

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
   * @see #eventSelectionChanged()
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

  Collection<Integer> getSelectedIndexes();

  /**
   * Sets the selected item
   * @param item the item to select
   */
  void setSelectedItem(final T item);

  /**
   * Selects all visible entities
   * @see #eventSelectionChanged()
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
   * @see #eventSelectionChanged()
   */
  void moveSelectionDown();

  /**
   * Moves all selected indexes up one index, wraps around
   * @see #eventSelectionChanged()
   */
  void moveSelectionUp();

  /**
   * @return the selection model used by this table model
   */
  ListSelectionModel getSelectionModel();

  interface SortingState {
    int getColumnIndex();
    SortingDirective getDirective();
  }
}
