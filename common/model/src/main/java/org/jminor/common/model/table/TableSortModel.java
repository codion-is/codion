/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.EventListener;

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
  void sort(final List<R> items);

  /**
   * @param columnIdentifier the identifier of the column to sort by
   * @param directive the sorting directive
   * @param addColumnToSort if false then the sorting state is cleared, otherwise
   * this column is added to the sorted column set according to sorting priority
   * @see #getSortingState(Object)
   */
  void setSortingDirective(final C columnIdentifier, final SortingDirective directive,
                           final boolean addColumnToSort);

  /**
   * @param columnIdentifier the column identifier
   * @return the {@link SortingState} associated with the given column
   */
  SortingState getSortingState(final C columnIdentifier);

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
  Class getColumnClass(final C columnIdentifier);

  /**
   * @param listener a listener notified each time the sorting state changes
   */
  void addSortingStateChangedListener(final EventListener listener);

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
