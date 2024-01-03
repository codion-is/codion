/*
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import javax.swing.SortOrder;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Handles the column sorting states for a {@link FilteredTableModel}.
 * @param <R> the type representing a row in the table model
 * @param <C> the type representing the column identifiers in the table model
 */
public interface FilteredTableSortModel<R, C> {

  /**
   * @return a {@link Comparator} based on this sort model
   */
  Comparator<R> comparator();

  /**
   * Clears the sorting state and adds the given column sorting order.
   * @param columnIdentifier the identifier of the column to sort by
   * @param sortOrder the sorting order
   * @throws IllegalStateException in case sorting is disabled for the given column
   * @see #addSortOrder(Object, SortOrder)
   * @see #sortOrder(Object)
   * @see #setSortingEnabled(Object, boolean)
   */
  void setSortOrder(C columnIdentifier, SortOrder sortOrder);

  /**
   * Adds the given column sorting order to the currently sorted columns.
   * If no column sorting is enabled, this call is the equivilent to using
   * {@link #setSortOrder(Object, SortOrder)}.
   * @param columnIdentifier the identifier of the column to sort by
   * @param sortOrder the sorting order
   * @throws IllegalStateException in case sorting is disabled for the given column
   * @see #setSortOrder(Object, SortOrder)
   * @see #sortOrder(Object)
   * @see #setSortingEnabled(Object, boolean)
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
  boolean sorted();

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
   * Disabling sorting will cause {@link #setSortOrder(Object, SortOrder)} and
   * {@link #addSortOrder(Object, SortOrder)} to throw a {@link IllegalStateException} for the given column.
   * @param columnIdentifier the column identifier
   * @param sortingEnabled true if sorting should be enabled for the given column
   */
  void setSortingEnabled(C columnIdentifier, boolean sortingEnabled);

  /**
   * @param columnIdentifier the column identifier
   * @return true if sorting is enabled for the given column
   */
  boolean isSortingEnabled(C columnIdentifier);

  /**
   * @param listener a listener notified each time the sorting state changes, with the column identifier as event data
   */
  void addSortingChangedListener(Consumer<C> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSortingChangedListener(Consumer<C> listener);

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

  /**
   * {@link SortOrder#ASCENDING} to {@link SortOrder#DESCENDING} to {@link SortOrder#UNSORTED} to {@link SortOrder#ASCENDING}.
   * @param currentSortOrder the current sort order
   * @return the next sort order
   */
  static SortOrder nextSortOrder(SortOrder currentSortOrder) {
    requireNonNull(currentSortOrder);
    switch (currentSortOrder) {
      case UNSORTED:
        return SortOrder.ASCENDING;
      case ASCENDING:
        return SortOrder.DESCENDING;
      case DESCENDING:
        return SortOrder.UNSORTED;
      default:
        throw new IllegalStateException("Unknown sort order: " + currentSortOrder);
    }
  }
}
