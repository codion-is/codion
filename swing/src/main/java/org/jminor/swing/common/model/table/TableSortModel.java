/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.EventListener;

import javax.swing.table.TableColumn;
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
  void sort(final List<R> items);

  /**
   * @param columnIdentifier the identifier of the column to sort by
   * @param directive the sorting directive
   * @param addColumnToSort if false then the sorting state is cleared, otherwise
   * this column is added to the sorted column set according to {@code getSortingPriority()}
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
   * @return true if sorting is enabled for one or more columns
   */
  boolean isSortingEnabled();

  /**
   * @return a list containing all the columns this sort model is based on
   */
  List<TableColumn> getColumns();

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
     * @return the sorting priority, 0 being the lowest
     */
    int getPriority();
  }
}
