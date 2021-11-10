/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventListener;

import javax.swing.SortOrder;
import java.util.List;

/**
 * Handles the column sorting states for table models
 * @param <R> the type representing a row in the table model
 * @param <C> the type representing the column identifiers in the table model
 */
public interface TableSortModel<R, C> {

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
   * @see #getSortingState(Object)
   */
  void setSortOrder(C columnIdentifier, SortOrder sortOrder);

  /**
   * Adds the given column sorting order to the currently sorted columns.
   * If no column sorting is enabled, this call is the equivilent to using
   * {@link #setSortOrder(Object, SortOrder)}.
   * @param columnIdentifier the identifier of the column to sort by
   * @param sortOrder the sorting order
   * @see #setSortOrder(Object, SortOrder)
   * @see #getSortingState(Object)
   */
  void addSortOrder(C columnIdentifier, SortOrder sortOrder);

  /**
   * @param columnIdentifier the column identifier
   * @return the {@link SortingState} associated with the given column
   */
  SortingState getSortingState(C columnIdentifier);

  /**
   * @return true if sorting is enabled for one or more columns
   */
  boolean isSortingEnabled();

  /**
   * Returns the class of the column with the given identifier
   * @param columnIdentifier the column identifier
   * @return the Class representing the given column
   */
  Class<?> getColumnClass(C columnIdentifier);

  /**
   * @param listener a listener notified each time the sorting state changes
   */
  void addSortingChangedListener(EventListener listener);

  /**
   * Specifies a sorting state for a column.
   */
  interface SortingState {

    /**
     * @return the sorting order currently associated with the column
     */
    SortOrder getSortOrder();

    /**
     * @return the sorting priority, 0 for first, 1 for second etc.
     */
    int getPriority();
  }
}
