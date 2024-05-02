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

import is.codion.common.event.EventObserver;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValues;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.model.table.TableSummaryModel;
import is.codion.common.value.Value;

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
 * @see #builder(ColumnFactory, ColumnValues)
 */
public interface FilteredTableModel<R, C> extends TableModel, FilteredModel<R> {

	/**
	 * Specifies how the data in a table model is refreshed.
	 */
	enum RefreshStrategy {
		/**
		 * Clear the table model before populating it with the refreshed data.
		 * This causes an empty selection event to be triggered, since the
		 * selection is cleared when the table model is cleared.
		 * @see FilteredTableSelectionModel#selectionEvent()
		 */
		CLEAR,
		/**
		 * Merges the refreshed data with the data already in the table model,
		 * by removing rows that are missing, replacing existing rows and adding new ones.
		 * This strategy does not cause an empty selection event to be triggered
		 * but at a considerable performance cost.
		 * Note that sorting is not performed using this strategy, since that would
		 * cause an empty selection event as well.
		 */
		MERGE
	}

	/**
	 * @return an observer notified each time the table data changes
	 */
	EventObserver<?> dataChangedEvent();

	/**
	 * @return an observer notified each time the table model is cleared
	 */
	EventObserver<?> clearedEvent();

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
	 * @return a {@link Export} instance for exporting the table model data
	 */
	Export export();

	/**
	 * Default {@link RefreshStrategy#CLEAR}
	 * @return the Value controlling the refresh strategy
	 */
	Value<RefreshStrategy> refreshStrategy();

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
	 * ({@link #refreshStrategy()}) will prevent that at a considerable performance cost.
	 * @see #refreshStrategy()
	 * @see RefreshStrategy
	 */
	@Override
	void refresh();

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * Retains the selection and filtering. Sorts the refreshed data unless merging on refresh is enabled.
	 * Note that an empty selection event will be triggered during a normal refresh, since the model is cleared
	 * before it is repopulated, during which the selection is cleared as well. Using merge on refresh
	 * ({@link #refreshStrategy()}) will prevent that at a considerable performance cost.
	 * @see #refreshStrategy()
	 * @see RefreshStrategy
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
	 * @param columnValues the column value provider
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @return a new builder instance
	 * @throws NullPointerException in case {@code columnFactory} or {@code columnValues} is null
	 */
	static <R, C> Builder<R, C> builder(ColumnFactory<C> columnFactory, ColumnValues<R, C> columnValues) {
		return new DefaultFilteredTableModel.DefaultBuilder<>(columnFactory, columnValues);
	}

	/**
	 * Instantiates a new {@link SummaryValues} instance.
	 * @param columnIdentifier the column identifier
	 * @param tableModel the table model
	 * @param format the format
	 * @param <T> the column value type
	 * @param <C> the column identifier type
	 * @return a new {@link SummaryValues} instance
	 */
	static <T extends Number, C> SummaryValues<T> summaryValues(C columnIdentifier, FilteredTableModel<?, C> tableModel, Format format) {
		return new DefaultFilteredTableModel.DefaultSummaryValues<>(columnIdentifier, tableModel, format);
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
		 * @param summaryValuesFactory the column summary values factory
		 * @return this builder instance
		 */
		Builder<R, C> summaryValuesFactory(SummaryValues.Factory<C> summaryValuesFactory);

		/**
		 * @param items supplies the items
		 * @return this builder instance
		 */
		Builder<R, C> items(Supplier<Collection<R>> items);

		/**
		 * Items failing validation can not be added to the model.
		 * @param itemValidator the item validator
		 * @return this builder instance
		 */
		Builder<R, C> itemValidator(Predicate<R> itemValidator);

		/**
		 * @param refreshStrategy the refresh strategy to use
		 * @return this builder instance
		 * @see FilteredTableModel#refresh()
		 */
		Builder<R, C> refreshStrategy(RefreshStrategy refreshStrategy);

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
	 * Exports the table data to a String.
	 */
	interface Export {
		/**
		 * @param delimiter the column delimiter (TAB by default)
		 * @return this Export instance
		 */
		Export delimiter(char delimiter);

		/**
		 * @param header include a column header
		 * @return this Export instance
		 */
		Export header(boolean header);

		/**
		 * @param hidden include hidden columns
		 * @return this Export instance
		 */
		Export hidden(boolean hidden);

		/**
		 * @param selected include only selected rows (default false)
		 * @return this Export instance
		 */
		Export selected(boolean selected);

		/**
		 * @return the table data exported to a String
		 */
		String get();
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
	interface ColumnValues<R, C> {

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
