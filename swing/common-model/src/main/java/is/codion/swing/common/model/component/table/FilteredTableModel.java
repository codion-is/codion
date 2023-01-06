/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.model.table.ColumnSummaryModel;

import javax.swing.table.TableModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Specifies a table model supporting selection as well as filtering
 * @param <R> the type representing the rows in this table model
 * @param <C> the type used to identify columns in this table model, Integer for indexed identification for example
 */
public interface FilteredTableModel<R, C> extends TableModel, FilteredModel<R> {

  /**
   * @param listener a listener to be notified each time the model has been sorted
   */
  void addSortListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSortListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the table data changes
   */
  void addDataChangedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeDataChangedListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the table model is cleared
   */
  void addClearListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeClearListener(EventListener listener);

  /**
   * Adds a listener that is notified each time rows are removed from this model.
   * @param listener the listener
   */
  void addRowsRemovedListener(EventDataListener<RowsRemoved> listener);

  /**
   * @param listener the listener to remove
   */
  void removeRowsRemovedListener(EventDataListener<RowsRemoved> listener);

  /**
   * @return true if an impending selection change should be allowed
   */
  boolean allowSelectionChange();

  /**
   * @param item the item
   * @return the index of the item in the table model
   */
  int indexOf(R item);

  /**
   * @param rowIndex the row index
   * @return the item at the given row index in the table model
   */
  R itemAt(int rowIndex);

  /**
   * Returns a String representation of the value for the given row and column.
   * @param rowIndex the row index
   * @param columnIdentifier the column identifier
   * @return the string value
   */
  String getStringAt(int rowIndex, C columnIdentifier);

  /**
   * Removes the given items from this table model
   * @param items the items to remove from the model
   */
  void removeItems(Collection<R> items);

  /**
   * Removes the given item from this table model
   * @param item the item to remove from the model
   */
  void removeItem(R item);

  /**
   * Removes from this table model the visible element whose index is between index
   * @param index the index of the row to be removed
   * @throws IndexOutOfBoundsException in case the indexe is out of bounds
   */
  void removeItemAt(int index);

  /**
   * Removes from this table model all visible elements whose index is between fromIndex, inclusive and toIndex, exclusive
   * @param fromIndex index of first row to be removed
   * @param toIndex index after last row to be removed
   * @throws IndexOutOfBoundsException in case the indexes are out of bounds
   */
  void removeItems(int fromIndex, int toIndex);

  /**
   * @return the FilteredTableColumnModel used by this TableModel
   */
  FilteredTableColumnModel<C> columnModel();

  /**
   * Returns the {@link ColumnSummaryModel} associated with {@code columnIdentifier}
   * @param columnIdentifier the column identifier
   * @return the ColumnSummaryModel for the column identified by the given identifier, an empty Optional if none is available
   */
  Optional<ColumnSummaryModel> columnSummaryModel(C columnIdentifier);

  /**
   * @return the column filter models, mapped to their respective column identifier
   */
  Map<C, ColumnFilterModel<R, C, ?>> columnFilterModels();

  /**
   * Returns the {@link ColumnConditionModel} for the column with the given identifier.
   * @param <T> the column value type
   * @param columnIdentifier the column identifier
   * @return the ColumnConditionModel for the column with the given identifier.
   * @throws IllegalArgumentException in case no filter model exists for the given column
   */
  <T> ColumnFilterModel<R, C, T> columnFilterModel(C columnIdentifier);

  /**
   * @param columnIdentifier the identifier of the column for which to retrieve the values
   * @param <T> the value type
   * @return the values (including nulls) of the column identified by the given identifier from the visible rows in the table model
   */
  <T> Collection<T> values(C columnIdentifier);

  /**
   * Returns the class of the column with the given identifier
   * @param columnIdentifier the column identifier
   * @return the Class representing the given column
   */
  Class<?> getColumnClass(C columnIdentifier);

  /**
   * @param columnIdentifier the identifier of the column for which to retrieve the values
   * @param <T> the value type
   * @return the values (including nulls) of the column identified by the given identifier from the selected rows in the table model
   */
  <T> Collection<T> selectedValues(C columnIdentifier);

  /**
   * @return true if merge on refresh is enabled
   */
  boolean isMergeOnRefresh();

  /**
   * @param mergeOnRefresh true if merge on refresh should be enabled
   */
  void setMergeOnRefresh(boolean mergeOnRefresh);

  /**
   * Sorts the visible items according to the {@link FilteredTableSortModel}, keeping the selected items.
   * Calling this method with the sort model disabled has no effect.
   * @see #sortModel()
   * @see #addSortListener(EventListener)
   * @see FilteredTableSortModel#isSortingEnabled
   */
  void sortItems();

  /**
   * @return the selection model used by this table model
   */
  FilteredTableSelectionModel<R> selectionModel();

  /**
   * @return the sorting model
   */
  FilteredTableSortModel<R, C> sortModel();

  /**
   * @return the search model
   */
  FilteredTableSearchModel searchModel();

  /**
   * Refreshes the items in this table model, respecting the selection, filtering as well as sorting states.
   * If run on the Event Dispatch Thread the refresh happens asynchronously, unless async refresh has been disabled
   * via {@link #setAsyncRefresh(boolean)}.
   * Note that an empty selection event will be triggered during a normal refresh, since the model is cleared
   * before it is repopulated, during which the selection is cleared as well. Using merge on insert
   * ({@link #setMergeOnRefresh(boolean)}) will prevent that at a considerable performance cost.
   * @throws RuntimeException in case of an exception when refresh is run synchronously
   * @see #addRefreshFailedListener(EventDataListener)
   * @see #setAsyncRefresh(boolean)
   */
  void refresh();

  /**
   * Clears all items from this table model
   */
  void clear();

  /**
   * Provides the column value for a row and column
   * @param <R> the row type
   * @param <C> the column identifier type
   */
  interface ColumnValueProvider<R, C> {

    /**
     * A Comparator for comparing {@link Comparable} instances.
     */
    Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = Comparable::compareTo;

    /**
     * A Comparator for comparing Objects according to their toString() value.
     */
    Comparator<?> TO_STRING_COMPARATOR = Comparator.comparing(Object::toString);

    /**
     * Returns the class of the column with the given identifier
     * @param columnIdentifier the column identifier
     * @return the Class representing the given column
     */
    Class<?> columnClass(C columnIdentifier);

    /**
     * Returns the comparator to use when sorting by the give column,
     * the comparator receives the column values, but never null.
     * @param columnIdentifier the column identifier
     * @return the comparator to use when sorting by the given column
     */
    default Comparator<?> comparator(C columnIdentifier) {
      if (Comparable.class.isAssignableFrom(columnClass(columnIdentifier))) {
        return COMPARABLE_COMPARATOR;
      }

      return TO_STRING_COMPARATOR;
    }

    /**
     * Returns a value for the given row and columnIdentifier
     * @param row the object representing a given row
     * @param columnIdentifier the column identifier
     * @return a value for the given row and column
     */
    Object value(R row, C columnIdentifier);

    /**
     * Returns a String representation of the value for the given row and columnIdentifier
     * @param row the row
     * @param columnIdentifier the column identifier
     * @return a String representation of the value for the given row and column
     */
    default String string(R row, C columnIdentifier) {
      Object columnValue = value(row, columnIdentifier);

      return columnValue == null ? "" : columnValue.toString();
    }
  }

  /**
   * Specifies the from and to rows of a row removal operation.
   */
  interface RowsRemoved {

    /**
     * @return the from row index
     */
    int fromRow();

    /**
     * @return the to row index
     */
    int toRow();
  }
}
