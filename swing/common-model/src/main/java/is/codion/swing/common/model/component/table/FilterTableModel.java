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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.model.FilterModel;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.value.Value;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.swing.common.model.component.table.DefaultFilterTableModel.COMPARABLE_COMPARATOR;
import static is.codion.swing.common.model.component.table.DefaultFilterTableModel.STRING_COMPARATOR;

/**
 * Specifies a table model supporting selection as well as filtering.
 * A {@link FilterTableModel} can not contain null items.
 * @param <R> the type representing the rows in this table model
 * @param <C> the type used to identify columns in this table model, Integer for indexed identification for example
 * @see #builder(TableColumns)
 */
public interface FilterTableModel<R, C> extends TableModel, FilterModel<R> {

	/**
	 * Specifies how the data in a table model is refreshed.
	 */
	enum RefreshStrategy {
		/**
		 * Clear the table model before populating it with the refreshed data.
		 * This causes an empty selection event to be triggered, since the
		 * selection is cleared when the table model is cleared.
		 * @see TableSelection#indexes()
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
	 * @return the table columns
	 */
	TableColumns<R, C> columns();

	/**
	 * Returns the class of the column with the given identifier
	 * @param identifier the column identifier
	 * @return the Class representing the given column
	 */
	Class<?> getColumnClass(C identifier);

	@Override
	FilterTableModelItems<R> items();

	/**
	 * Provides access to column values
	 * @return the {@link ColumnValues}
	 */
	ColumnValues<C> values();

	/**
	 * @return the {@link TableSelection} instance used by this table model
	 */
	TableSelection<R> selection();

	/**
	 * @return the {@link TableConditionModel} used to filter this table model
	 */
	TableConditionModel<C> filters();

	/**
	 * @return the sort model
	 */
	FilterTableSortModel<R, C> sort();

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
	 * @param columns the columns
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @return a new builder instance
	 * @throws NullPointerException in case {@code columnValues} is null
	 */
	static <R, C> Builder<R, C> builder(TableColumns<R, C> columns) {
		return new DefaultFilterTableModel.DefaultBuilder<>(columns);
	}

	/**
	 * @param <R> the row type
	 */
	interface FilterTableModelItems<R> extends FilterModel.Items<R> {

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
		void refresh(Consumer<Collection<R>> onResult);

		/**
		 * Default {@link RefreshStrategy#CLEAR}
		 * @return the {@link Value} controlling the refresh strategy
		 */
		Value<RefreshStrategy> refreshStrategy();
	}

	/**
	 * Provides access to table column values
	 * @param <C> the column identifier type
	 */
	interface ColumnValues<C> {

		/**
		 * @param identifier the identifier of the column for which to retrieve the values
		 * @param <T> the value type
		 * @return the values (including nulls) of the column identified by the given identifier from the visible rows in the table model
		 * @throws IllegalArgumentException in case of an unknown identifier
		 */
		<T> Collection<T> get(C identifier);

		/**
		 * @param identifier the identifier of the column for which to retrieve the selected values
		 * @param <T> the value type
		 * @return the values (including nulls) of the column identified by the given identifier from the selected rows in the table model
		 * @throws IllegalArgumentException in case of an unknown identifier
		 */
		<T> Collection<T> selected(C identifier);

		/**
		 * Returns the value for the given row and column.
		 * @param rowIndex the row index
		 * @param identifier the column identifier
		 * @return the value
		 * @see TableColumns#value(Object, Object)
		 */
		Object value(int rowIndex, C identifier);

		/**
		 * Returns a string representation of the value for the given row and column, an empty string in case of null.
		 * @param rowIndex the row index
		 * @param identifier the column identifier
		 * @return the string value or an empty string in case of null
		 * @see TableColumns#string(Object, Object)
		 */
		String string(int rowIndex, C identifier);
	}

	/**
	 * A builder for a {@link FilterTableModel}.
	 * @param <R> the row type
	 * @param <C> the column identifer type
	 */
	interface Builder<R, C> {

		/**
		 * @param filterModelFactory the column filter model factory
		 * @return this builder instance
		 */
		Builder<R, C> filterModelFactory(Supplier<Map<C, ConditionModel<?>>> filterModelFactory);

		/**
		 * @param supplier supplies the items
		 * @return this builder instance
		 */
		Builder<R, C> supplier(Supplier<? extends Collection<R>> supplier);

		/**
		 * Items failing validation can not be added to the model.
		 * @param validator the item validator
		 * @return this builder instance
		 */
		Builder<R, C> validator(Predicate<R> validator);

		/**
		 * @param refreshStrategy the refresh strategy to use
		 * @return this builder instance
		 * @see FilterTableModel.Items#refresh()
		 */
		Builder<R, C> refreshStrategy(RefreshStrategy refreshStrategy);

		/**
		 * @param asyncRefresh true if async refresh should be enabled
		 * @return this builder instance
		 */
		Builder<R, C> asyncRefresh(boolean asyncRefresh);

		/**
		 * @return a new {@link FilterTableModel} instance.
		 */
		FilterTableModel<R, C> build();
	}

	/**
	 * Specifies the columns for a table model, their identifiers,
	 * their class and how to extract their value from a row instance.
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 */
	interface TableColumns<R, C> {

		/**
		 * This method gets called quite often, so it is recommended to return
		 * a constant List instance, instead of creating one each time.
		 * @return the column identifiers
		 */
		List<C> identifiers();

		/**
		 * @param identifier the column identifier
		 * @return the column class for the given column
		 */
		Class<?> columnClass(C identifier);

		/**
		 * Returns a value for the given row and identifier
		 * @param row the object representing a given row
		 * @param identifier the column identifier
		 * @return a value for the given row and column
		 */
		Object value(R row, C identifier);

		/**
		 * @param index the identifier index
		 * @return the identifier at the given index
		 */
		default C identifier(int index) {
			return identifiers().get(index);
		}

		/**
		 * Returns a String representation of the value for the given row and column,
		 * an empty String in case of null.
		 * @param row the row
		 * @param identifier the column identifier
		 * @return a String representation of the value for the given row and column, an empty String in case of null
		 */
		default String string(R row, C identifier) {
			Object columnValue = value(row, identifier);

			return columnValue == null ? "" : columnValue.toString();
		}

		/**
		 * Returns a Comparable instance for the given row and column.
		 * <p>
		 * Null is returned if the underlying column value is not a {@link Comparable} instance.
		 * @param <T> the column value type
		 * @param row the object representing a given row
		 * @param identifier the column identifier
		 * @return a Comparable for the given row and column
		 */
		default <T> Comparable<T> comparable(R row, C identifier) {
			Object value = value(row, identifier);
			if (value instanceof Comparable) {
				return (Comparable<T>) value;
			}

			return null;
		}

		/**
		 * Returns the comparator to use when comparing the values of the given column
		 * @param identifier the column identifier
		 * @return a Comparator for the given column
		 */
		default Comparator<?> comparator(C identifier) {
			if (Comparable.class.isAssignableFrom(columnClass(identifier))) {
				return COMPARABLE_COMPARATOR;
			}

			return STRING_COMPARATOR;
		}
	}

	/**
	 * A selection model for a {@link FilterTableModel}.
	 * @param <R> the type of rows
	 */
	interface TableSelection<R> extends ListSelectionModel, MultiSelection<R> {}
}
