/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.EventDataListener;

import javax.swing.SortOrder;
import java.util.List;

/**
 * Handles the column sorting states for a {@link FilteredTableModel}.
 * @param <R> the type representing a row in the table model
 * @param <C> the type representing the column identifiers in the table model
 */
public interface FilteredTableSortModel<R, C> {

  /**
   * Sorts the given list according to the sort configuration
   * @param items the items to sort
   */
  void sort(List<R> items);

  /**
   * Clears the sorting state and adds the given column sorting order.
   * @param columnIdentifier the identifier of the column to sort by
   * @param sortOrder the sorting order
   * @see #addSortOrder(Object, SortOrder)
   * @see #sortOrder(Object)
   */
  void setSortOrder(C columnIdentifier, SortOrder sortOrder);

  /**
   * Adds the given column sorting order to the currently sorted columns.
   * If no column sorting is enabled, this call is the equivilent to using
   * {@link #setSortOrder(Object, SortOrder)}.
   * @param columnIdentifier the identifier of the column to sort by
   * @param sortOrder the sorting order
   * @see #setSortOrder(Object, SortOrder)
   * @see #sortOrder(Object)
   */
  void addSortOrder(C columnIdentifier, SortOrder sortOrder);

  /**
   * @param columnIdentifier the column identifier
   * @return the {@link SortOrder} associated with the given column
   */
  SortOrder sortOrder(C columnIdentifier);

  /**
   * @param columnIdentifier the column identifier
   * @return the sort priority for the given column, -1 if not sorted
   */
  int sortPriority(C columnIdentifier);

  /**
   * @return true if sorting is enabled for one or more columns
   */
  boolean isSortingEnabled();

  /**
   * Returns the current column sort order, in order of priority
   * @return the current column sort order, in order of priority
   */
  List<ColumnSortOrder<C>> columnSortOrder();

  /**
   * Clears the sorting states from this sort model. Note that only one sorting change event
   * will happen, with the first sort column.
   */
  void clear();

  /**
   * @param listener a listener notified each time the sorting state changes, with the column identifier as event data
   */
  void addSortingChangedListener(EventDataListener<C> listener);

  /**
   * Specifies a sorting state for a column.
   */
  interface ColumnSortOrder<C> {

    /**
     * @return the column identifier
     */
    C columnIdentifier();

    /**
     * @return the sorting order currently associated with the column
     */
    SortOrder sortOrder();
  }
}
