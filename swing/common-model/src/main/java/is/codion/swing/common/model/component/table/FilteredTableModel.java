/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.model.table.TableSummaryModel;
import is.codion.common.state.State;

import javax.swing.table.TableModel;
import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Specifies a table model supporting selection as well as filtering
 * @param <R> the type representing the rows in this table model
 * @param <C> the type used to identify columns in this table model, Integer for indexed identification for example
 * @see #builder(ColumnFactory, ColumnValueProvider)
 */
public interface FilteredTableModel<R, C> extends TableModel, FilteredModel<R> {

	/**
	 * @param listener a listener to be notified each time the table data changes
	 */
	void addDataChangedListener(Runnable listener);

	/**
	 * @param listener the listener to remove
	 */
	void removeDataChangedListener(Runnable listener);

	/**
	 * @param listener a listener to be notified each time the table model is cleared
	 */
	void addClearListener(Runnable listener);

	/**
	 * @param listener the listener to remove
	 */
	void removeClearListener(Runnable listener);

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
	 * @see FilteredTableSortModel#sorted()
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
	 * @see #includeCondition()
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
	 * @return the removed item
	 * @throws IndexOutOfBoundsException in case the indexe is out of bounds
	 */
	R removeItemAt(int index);

	/**
	 * Removes from this table model all visible elements whose index is between fromIndex, inclusive and toIndex, exclusive
	 * @param fromIndex index of first row to be removed
	 * @param toIndex index after last row to be removed
	 * @return the removed items
	 * @throws IndexOutOfBoundsException in case the indexes are out of bounds
	 */
	List<R> removeItems(int fromIndex, int toIndex);

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
	 * Note that when merging during refresh, the items are not sorted, since that
	 * would cause an empty-selection event, defeating the purpose of merging.
	 * @return the State controlling whether merge on refresh should be enabled
	 */
	State mergeOnRefresh();

	/**
	 * Sorts the visible items according to the {@link FilteredTableSortModel}, keeping the selected items.
	 * Calling this method with the sort model disabled has no effect.
	 * @see #sortModel()
	 * @see FilteredTableSortModel#sorted
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
	 * {@inheritDoc}
	 * <br><br>
	 * Retains the selection and filtering. Sorts the refreshed data unless merging on refresh is enabled.
	 * Note that an empty selection event will be triggered during a normal refresh, since the model is cleared
	 * before it is repopulated, during which the selection is cleared as well. Using merge on refresh
	 * ({@link #mergeOnRefresh()}) will prevent that at a considerable performance cost.
	 * @see #mergeOnRefresh()
	 */
	@Override
	void refresh();

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Retains the selection and filtering. Sorts the refreshed data unless merging on refresh is enabled.
	 * Note that an empty selection event will be triggered during a normal refresh, since the model is cleared
	 * before it is repopulated, during which the selection is cleared as well. Using merge on refresh
	 * ({@link #mergeOnRefresh()}) will prevent that at a considerable performance cost.
	 * @param afterRefresh called after a successful refresh, may be null
	 * @see #mergeOnRefresh()
	 */
	@Override
	void refreshThen(Consumer<Collection<R>> afterRefresh);

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
	 * @param columnFactory the column factory
	 * @param columnValueProvider the column value provider
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @return a new builder instance
	 * @throws NullPointerException in case {@code columnFactory} or {@code columnValueProvider} is null
	 */
	static <R, C> Builder<R, C> builder(ColumnFactory<C> columnFactory, ColumnValueProvider<R, C> columnValueProvider) {
		return new DefaultFilteredTableModel.DefaultBuilder<>(columnFactory, columnValueProvider);
	}

	/**
	 * Instantiates a new {@link SummaryValueProvider} instance.
	 * @param columnIdentifier the column identifier
	 * @param tableModel the table model
	 * @param format the format
	 * @param <T> the column value type
	 * @param <C> the column identifier type
	 * @return a new {@link SummaryValueProvider} instance
	 */
	static <T extends Number, C> SummaryValueProvider<T> summaryValueProvider(C columnIdentifier, FilteredTableModel<?, C> tableModel, Format format) {
		return new DefaultFilteredTableModel.DefaultSummaryValueProvider<>(columnIdentifier, tableModel, format);
	}

	/**
	 * A builder for a {@link FilteredTableModel}.
	 * @param <R> the row type
	 * @param <C> the column identifer type
	 */
	interface Builder<R, C> {

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
		 * @param itemSupplier the item supplier
		 * @return this builder instance
		 */
		Builder<R, C> itemSupplier(Supplier<Collection<R>> itemSupplier);

		/**
		 * Items failing validation can not be added to the model.
		 * @param itemValidator the item validator
		 * @return this builder instance
		 */
		Builder<R, C> itemValidator(Predicate<R> itemValidator);

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
	 * Provides columns for a {@link FilteredTableModel}.
	 * @param <C> the column identifier type
	 */
	interface ColumnFactory<C> {

		/**
		 * @return the columns, may not be empty
		 */
		List<FilteredTableColumn<C>> createColumns();
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
		 * Returns a String representation of the value for the given row and columnIdentifier,
		 * an empty String in case of null.
		 * @param row the row
		 * @param columnIdentifier the column identifier
		 * @return a String representation of the value for the given row and column, an empty String in case of null
		 */
		default String string(R row, C columnIdentifier) {
			Object columnValue = value(row, columnIdentifier);

			return columnValue == null ? "" : columnValue.toString();
		}

		/**
		 * Returns a Comparable instance for the given row and columnIdentifier.
		 * The default implementation returns the value as is in case it's a {@link Comparable} instance,
		 * otherwise null is returned.
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

			return null;
		}
	}
}
