/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventListener;

import java.util.List;

/**
 * Handles the column sorting states for table models
 * @param <R> the type representing a row in the table model
 * @param <C> the type representing the column identifiers in the table model
 * @param <T> the type representing table columns
 */
public interface TableSortModel<R, C, T> {

  /**
   * Sorts the given list according to the sort configuration
   * @param items the items to sort
   */
  void sort(List<R> items);

  /**
   * Clears the sorting state and adds the given column sorting directive.
   * @param columnIdentifier the identifier of the column to sort by
   * @param directive the sorting directive
   * @see #addSortingDirective(Object, SortingDirective)
   * @see #getSortingState(Object)
   */
  void setSortingDirective(C columnIdentifier, SortingDirective directive);

  /**
   * Adds the given column sorting directive to the currently sorted columns.
   * If no column sorting is enabled, this call is the equivilent to using
   * {@link #setSortingDirective(Object, SortingDirective)}.
   * @param columnIdentifier the identifier of the column to sort by
   * @param directive the sorting directive
   * @see #setSortingDirective(Object, SortingDirective)
   * @see #getSortingState(Object)
   */
  void addSortingDirective(C columnIdentifier, SortingDirective directive);

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
   * @return a list containing all the columns this sort model is based on
   */
  List<T> getColumns();

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
     * @return the sorting directive currently associated with the column
     */
    SortingDirective getDirective();

    /**
     * @return the sorting priority, 0 for first, 1 for second etc.
     */
    int getPriority();
  }
}
