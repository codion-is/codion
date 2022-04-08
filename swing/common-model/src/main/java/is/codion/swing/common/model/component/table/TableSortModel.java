/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.EventDataListener;

import javax.swing.SortOrder;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Handles the column sorting states for table models.
 * Instantiate with the {@link #create(ColumnClassProvider, ColumnValueProvider)} factory method.
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
   * Returns the current column sort order, in order of priority
   * @return the current column sort orderk, in order of priority
   */
  LinkedHashMap<C, SortOrder> getColumnSortOrder();

  /**
   * Returns the class of the column with the given identifier
   * @param columnIdentifier the column identifier
   * @return the Class representing the given column
   */
  Class<?> getColumnClass(C columnIdentifier);

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

  /**
   * Provides the column value to sort by
   * @param <R> the row type
   * @param <C> the column identifier type
   */
  interface ColumnValueProvider<R, C> {

    /**
     * Returns a value the given row and columnIdentifier, used for sorting
     * @param row the object representing a given row
     * @param columnIdentifier the column identifier
     * @return a value for the given row and column
     */
    Object getColumnValue(R row, C columnIdentifier);
  }

  /**
   * Provides the type class for a column
   * @param <C> the column identifier type
   */
  interface ColumnClassProvider<C> {

    /**
     * Returns the class of the column with the given identifier
     * @param columnIdentifier the column identifier
     * @return the Class representing the given column
     */
    Class<?> getColumnClass(C columnIdentifier);
  }

  /**
   * A factory for {@link Comparator} instances for columns
   * @param <C> the column identifier type
   */
  interface ColumnComparatorFactory<C> {

    /**
     * Creates a comparator used when sorting by the give column,
     * the comparator receives the column values, but never null.
     * @param columnIdentifier the column identifier
     * @param columnClass the column class
     * @return the comparator to use when sorting by the given column
     */
    Comparator<?> createComparator(C columnIdentifier, Class<?> columnClass);
  }

  /**
   * A factory for {@link TableSortModel} instances.
   * @param columnClassProvider the column class provider
   * @param columnValueProvider the column value provider
   * @param <R> the row type
   * @param <C> the column identifier type
   * @return a new {@link TableSortModel} instance
   */
  static <R, C> TableSortModel<R, C> create(ColumnClassProvider<C> columnClassProvider,
                                            ColumnValueProvider<R, C> columnValueProvider) {
    return new SwingTableSortModel<>(columnClassProvider, columnValueProvider);
  }

  /**
   * A factory for {@link TableSortModel} instances.
   * @param columnClassProvider the column class provider
   * @param columnValueProvider the column value provider
   * @param columnComparatorFactory the column comparatory factory
   * @param <R> the row type
   * @param <C> the column identifier type
   * @return a new {@link TableSortModel} instance
   */
  static <R, C> TableSortModel<R, C> create(ColumnClassProvider<C> columnClassProvider,
                                            ColumnValueProvider<R, C> columnValueProvider,
                                            ColumnComparatorFactory<C> columnComparatorFactory) {
    return new SwingTableSortModel<>(columnClassProvider, columnValueProvider, columnComparatorFactory);
  }
}
