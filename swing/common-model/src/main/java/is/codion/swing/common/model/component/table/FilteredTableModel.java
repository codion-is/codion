/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.model.table.TableSummaryModel;

import javax.swing.table.TableModel;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Specifies a table model supporting selection as well as filtering
 * @param <R> the type representing the rows in this table model
 * @param <C> the type used to identify columns in this table model, Integer for indexed identification for example
 * @see #builder(ColumnValueProvider)
 */
public interface FilteredTableModel<R, C> extends TableModel, FilteredModel<R> {

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
  void addRowsRemovedListener(EventDataListener<RemovedRows> listener);

  /**
   * @param listener the listener to remove
   */
  void removeRowsRemovedListener(EventDataListener<RemovedRows> listener);

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
  String getStringValueAt(int rowIndex, C columnIdentifier);

  /**
   * Adds the given items to the bottom of this table model.
   * @param items the items to add
   */
  void addItems(Collection<R> items);

  /**
   * Adds the given items to the bottom of this table model.
   * If sorting is enabled this model is sorted after the items have been added.
   * @param items the items to add
   */
  void addItemsSorted(Collection<R> items);

  /**
   * Adds the given items to this table model, non-filtered items are added at the given index.
   * @param index the index at which to add the items
   * @param items the items to add
   */
  void addItemsAt(int index, Collection<R> items);

  /**
   * Adds the given items to this table model, non-filtered items are added at the given index.
   * If sorting is enabled this model is sorted after the items have been added.
   * @param index the index at which to add the items
   * @param items the items to add
   * @see FilteredTableSortModel#isSorted()
   */
  void addItemsAtSorted(int index, Collection<R> items);

  /**
   * Adds the given item to the bottom of this table model.
   * @param item the item to add
   */
  void addItem(R item);

  /**
   * @param index the index
   * @param item the item to add
   */
  void addItemAt(int index, R item);

  /**
   * Adds the given item to the bottom of this table model.
   * If sorting is enabled this model is sorted after the item has been added.
   * @param item the item to add
   */
  void addItemSorted(R item);

  /**
   * Sets the item at the given index.
   * If the item should be filtered calling this method has no effect.
   * @param index the index
   * @param item the item
   * @see #setIncludeCondition(Predicate)
   */
  void setItemAt(int index, R item);

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
   * @param delimiter the delimiter
   * @return the table rows as a tab delimited string, with column names as a header
   */
  String rowsAsDelimitedString(char delimiter);

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
   * @see FilteredTableSortModel#isSorted
   */
  void sortItems();

  /**
   * @return the FilteredTableColumnModel used by this TableModel
   */
  FilteredTableColumnModel<C> columnModel();

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
   * @return the filter model used by this table model
   */
  TableConditionModel<C> filterModel();

  /**
   * @return the summary model
   */
  TableSummaryModel<C> summaryModel();

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
   * Notifies all listeners that all cell values in the table's rows may have changed.
   * The number of rows may also have changed and the JTable should redraw the table from scratch.
   * The structure of the table (as in the order of the columns) is assumed to be the same.
   */
  void fireTableDataChanged();

  /**
   * Notifies all listeners that the given rows have changed
   * @param fromIndex the from index
   * @param toIndex the to index
   */
  void fireTableRowsUpdated(int fromIndex, int toIndex);

  /**
   * Instantiates a new table model builder.
   * @param columnValueProvider the column value provider
   * @param <R> the row type
   * @param <C> the column identifier type
   * @return a new builder instance
   * @throws NullPointerException in case {@code columnValueProvider} is null
   */
  static <R, C> Builder<R, C> builder(ColumnValueProvider<R, C> columnValueProvider) {
    return new DefaultFilteredTableModel.DefaultBuilder<>(columnValueProvider);
  }

  /**
   * A builder for a {@link FilteredTableModel}.
   * @param <R> the row type
   * @param <C> the column identifer type
   */
  interface Builder<R, C> {

    /**
     * @param columns the columns
     * @return this builder instance
     * @throws IllegalArgumentException in case columns is empty
     */
    Builder<R, C> columns(List<FilteredTableColumn<C>> columns);

    /**
     * @param filterModelFactory the column filter model factory
     * @return this builder instance
     */
    Builder<R, C> filterModelFactory(ColumnConditionModel.Factory<C> filterModelFactory);

    /**
     * @param summaryValueProviderFactory the column summary value provider factory
     * @return this builder instance
     */
    Builder<R, C> summaryValueProviderFactory(SummaryValueProvider.Factory<C> summaryValueProviderFactory);

    /**
     * @param rowSupplier the row supplier
     * @return this builder instance
     */
    Builder<R, C> rowSupplier(Supplier<Collection<R>> rowSupplier);

    /**
     * Rows failing validation can not be added to the model.
     * @param rowValidator the row validator
     * @return this builder instance
     */
    Builder<R, C> rowValidator(Predicate<R> rowValidator);

    /**
     * @param mergeOnRefresh if true the merge on refresh is used
     * @return this builder instance
     */
    Builder<R, C> mergeOnRefresh(boolean mergeOnRefresh);

    /**
     * @param asyncRefresh true if async refresh should be enabled
     * @return this builder instance
     */
    Builder<R, C> asyncRefresh(boolean asyncRefresh);

    /**
     * @return a new {@link FilteredTableModel} instance.
     */
    FilteredTableModel<R, C> build();
  }

  /**
   * Provides the column value for a row and column
   * @param <R> the row type
   * @param <C> the column identifier type
   */
  interface ColumnValueProvider<R, C> {

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

    /**
     * Returns a Comparable instance for the given row and columnIdentifier.
     * The default implementation returns the value as is in case it's a {@link Comparable} instance,
     * otherwise its String representation is returned.
     * @param <T> the column value type
     * @param row the object representing a given row
     * @param columnIdentifier the column identifier
     * @return a Comparable for the given row and column
     */
    default <T> Comparable<T> comparable(R row, C columnIdentifier) {
      Object value = value(row, columnIdentifier);
      if (value instanceof Comparable) {
        return (Comparable<T>) value;
      }
      if (value != null) {
        return (Comparable<T>) value.toString();
      }

      return null;
    }
  }

  /**
   * Specifies the from and to rows of a row removal operation.
   */
  interface RemovedRows {

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
