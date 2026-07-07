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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.component.table;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.common.model.selection.MultiSelection;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.model.component.table.DefaultFilterTableModel.COMPARABLE_COMPARATOR;
import static is.codion.common.model.component.table.DefaultFilterTableModel.STRING_COMPARATOR;
import static java.util.Objects.requireNonNull;

/**
 * <p>A UI-agnostic table model based on {@link FilterModel}, supporting selection, filtering and sorting.
 * A {@link FilterTableModel} can not contain null items.
 * <p>The Swing-specific {@code is.codion.swing.common.model.component.table.SwingFilterTableModel} extends this with
 * {@code javax.swing.table.TableModel}, adding cell editing and the {@code fireTableXxx} surface.
 * @param <R> the type representing the rows in this table model
 * @param <C> the type used to identify columns in this table model, Integer for indexed identification for example
 * @see #builder()
 */
public interface FilterTableModel<R, C> extends FilterModel<R> {

	/**
	 * @return the table columns
	 */
	TableColumns<R, C> columns();

	/**
	 * Provides access to column values
	 * @return the {@link ColumnValues}
	 */
	ColumnValues<C> values();

	/**
	 * @return the {@link MultiSelection} instance used by this table model
	 */
	@Override
	MultiSelection<R> selection();

	/**
	 * @return the {@link TableConditionModel} used to filter this table model
	 */
	TableConditionModel<C> filters();

	/**
	 * @return the sort
	 */
	@Override
	FilterTableSort<R, C> sort();

	/**
	 * @return a {@link Export} instance for exporting the table model data
	 */
	Export<C> export();

	/**
	 * @return a {@link Builder.ColumnsStep} instance
	 */
	static Builder.ColumnsStep builder() {
		return DefaultFilterTableModel.DefaultBuilder.COLUMNS;
	}

	/**
	 * Provides access to table column values
	 * @param <C> the column identifier type
	 */
	interface ColumnValues<C> {

		/**
		 * @param identifier the identifier of the column for which to retrieve the values
		 * @param <T> the value type
		 * @return the values (including nulls) of the column identified by the given identifier from the included rows in the table model
		 * @throws IllegalArgumentException in case of an unknown identifier
		 */
		<T> List<T> get(C identifier);

		/**
		 * @param identifier the identifier of the column for which to retrieve the selected values
		 * @param <T> the value type
		 * @return the values (including nulls) of the column identified by the given identifier from the selected rows in the table model
		 * @throws IllegalArgumentException in case of an unknown identifier
		 */
		<T> List<T> selected(C identifier);

		/**
		 * Returns the value for the given row and column.
		 * @param rowIndex the row index
		 * @param identifier the column identifier
		 * @return the value
		 * @see TableColumns#value(Object, Object)
		 */
		@Nullable Object value(int rowIndex, C identifier);

		/**
		 * Returns a string representation of the value for the given row and column, an empty string in case of null.
		 * @param rowIndex the row index
		 * @param identifier the column identifier
		 * @return the string value or an empty string in case of null
		 * @see TableColumns#formatted(Object, Object)
		 */
		String formatted(int rowIndex, C identifier);
	}

	/**
	 * A builder for a {@link FilterTableModel}.
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 */
	interface Builder<R, C> {

		/**
		 * Provides a {@link Builder} instance
		 */
		interface ColumnsStep {

			/**
			 * @param <R> the type representing rows
			 * @param <C> the type used to identify columns
			 * @param columns the columns
			 * @return a {@link Builder} based on the given columns
			 * @throws NullPointerException in case {@code columns} is null
			 */
			<R, C> Builder<R, C> columns(TableColumns<R, C> columns);
		}

		/**
		 * @param filters the column filter model factory
		 * @return this builder instance
		 */
		Builder<R, C> filters(Supplier<Map<C, ConditionModel<?>>> filters);

		/**
		 * @param items supplies the items
		 * @return this builder instance
		 */
		Builder<R, C> items(Supplier<Collection<R>> items);

		/**
		 * Items failing validation can not be added to the model.
		 * @param validator the item validator
		 * @return this builder instance
		 */
		Builder<R, C> validator(Predicate<R> validator);

		/**
		 * By default, exceptions during refresh are rethrown,
		 * use this method to handle async exceptions differently
		 * @param onRefreshException the exception handler to use during refresh
		 * @return this builder instance
		 */
		Builder<R, C> onRefreshException(Consumer<Exception> onRefreshException);

		/**
		 * @param included the {@link Predicate} controlling which items should be included
		 * @return this builder instance
		 */
		Builder<R, C> included(Predicate<R> included);

		/**
		 * @param refresh true if the model items should be refreshed on initialization, false by default
		 * @return this builder instance
		 */
		Builder<R, C> refresh(boolean refresh);

		/**
		 * @param listener the selection listener
		 * @return this builder instance
		 */
		Builder<R, C> onSelectionChanged(Runnable listener);

		/**
		 * @param item receives the selected item
		 * @return this builder instance
		 */
		Builder<R, C> onItemSelected(Consumer<R> item);

		/**
		 * @param items receives the selected items
		 * @return this builder instance
		 */
		Builder<R, C> onItemsSelected(Consumer<List<R>> items);

		/**
		 * @param index receives the selected index
		 * @return this builder instance
		 */
		Builder<R, C> onIndexSelected(Consumer<Integer> index);

		/**
		 * @param indexes receives the selected indexes
		 * @return this builder instance
		 */
		Builder<R, C> onIndexesSelected(Consumer<List<Integer>> indexes);

		/**
		 * Provides the {@link MultiSelection} for this model, given its {@link IncludedItems}.
		 * The default is the pure-Java {@link MultiSelection#multiSelection(MultiSelection.IndexedItems)};
		 * the Swing layer plugs a {@code javax.swing.ListSelectionModel} based one.
		 * @param selection the selection factory
		 * @return this builder instance
		 */
		Builder<R, C> selection(Function<IncludedItems<R>, MultiSelection<R>> selection);

		/**
		 * Provides the {@link FilterModel.Refresher} for this model, given its {@link Items}.
		 * The default is a UI-agnostic synchronous refresher; the Swing layer plugs a {@code ProgressWorker}
		 * based one and Android a coroutine based one.
		 * @param refresher the refresher factory
		 * @return this builder instance
		 */
		Builder<R, C> refresher(Function<Items<R>, Refresher<R>> refresher);

		/**
		 * Adds an {@link ItemsListener} notified of fine-grained changes to the included items, allowing
		 * toolkit layers to bridge to their table change notifications (e.g. {@code TableModelEvent}s).
		 * @param itemsListener the {@link ItemsListener} to add
		 * @return this builder instance
		 */
		Builder<R, C> listener(ItemsListener itemsListener);

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
		@Nullable Object value(R row, C identifier);

		/**
		 * The default implementation simply returns {@code identifier.toString()}
		 * @param identifier the column identifier
		 * @return the caption for the given column
		 */
		default String caption(C identifier) {
			return requireNonNull(identifier).toString();
		}

		/**
		 * The default implementation returns an empty {@link Optional}.
		 * @param identifier the column identifier
		 * @return the description for the given column
		 */
		default Optional<String> description(C identifier) {
			requireNonNull(identifier);

			return Optional.empty();
		}

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
		default String formatted(R row, C identifier) {
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
		default <T> @Nullable Comparable<T> comparable(R row, C identifier) {
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
	 * Exports the table model data as a String.
	 */
	interface Export<C> {

		/**
		 * @param columns the columns to export, default all
		 * @return this Export instance
		 */
		Export<C> columns(List<C> columns);

		/**
		 * @param delimiter the column delimiter, TAB by default
		 * @return this Export instance
		 */
		Export<C> delimiter(char delimiter);

		/**
		 * @param header include a column header, default true
		 * @return this Export instance
		 */
		Export<C> header(boolean header);

		/**
		 * @param selected include only selected rows, default false
		 * @return this Export instance
		 */
		Export<C> selected(boolean selected);

		/**
		 * <p>Replaces newlines inside strings.
		 * <p>Note that strings are always trimmed, regardless of this setting, so newlines at the
		 * beginning and end of strings are removed before replacement is performed.
		 * <p>Default replacement is a single whitespace (" ").
		 * <p>Set to null to keep internal newlines in place.
		 * @param replacement the string to use when replacing newlines
		 * @return this Export instance
		 */
		Export<C> replaceNewline(@Nullable String replacement);

		/**
		 * @return the exported table data as a String
		 */
		String get();
	}
}
