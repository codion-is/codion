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
 * Copyright (c) 2013 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.component.table;

import is.codion.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.filter.FilterModel.IncludedItems;
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.common.model.selection.MultiSelection;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A base class for {@link FilterTableModel.Builder} implementations. A toolkit builder extends this class, adding
 * its own options and overriding {@link #build()} to wrap the {@link FilterTableModel} built by
 * {@link #build(Function, ItemsListener)} in its own model.
 * @param <R> the row type
 * @param <C> the column identifier type
 * @param <B> the builder type
 * @see #build(Function, ItemsListener)
 * @see #refresh()
 */
public abstract class AbstractFilterTableModelBuilder<R, C, B extends FilterTableModel.Builder<R, C, B>>
				implements FilterTableModel.Builder<R, C, B> {

	private static final ValidPredicate<Object> DEFAULT_VALID_PREDICATE = new ValidPredicate<>();

	final TableColumns<R, C> columns;
	final List<Runnable> selectionListeners = new ArrayList<>();
	final List<Consumer<R>> itemSelectedListeners = new ArrayList<>();
	final List<Consumer<List<R>>> itemsSelectedListeners = new ArrayList<>();
	final List<Consumer<Integer>> indexSelectedListeners = new ArrayList<>();
	final List<Consumer<List<Integer>>> indexesSelectedListeners = new ArrayList<>();

	@Nullable Supplier<Collection<R>> supplier;
	Predicate<R> validator = (Predicate<R>) DEFAULT_VALID_PREDICATE;
	Supplier<Map<C, ConditionModel<?>>> filters;
	@Nullable Consumer<Exception> onRefreshException;
	@Nullable Predicate<R> included;

	private boolean refresh = false;

	/**
	 * @param columns the columns
	 * @throws IllegalArgumentException in case the columns specify no identifiers, or non-unique ones
	 */
	protected AbstractFilterTableModelBuilder(TableColumns<R, C> columns) {
		if (requireNonNull(columns).identifiers().isEmpty()) {
			throw new IllegalArgumentException("TableColumns does not specify any column identifiers");
		}
		this.columns = validateIdentifiers(columns);
		this.filters = new DefaultFilterTableModel.DefaultColumnFilterFactory<>(columns);
	}

	@Override
	public final B filters(Supplier<Map<C, ConditionModel<?>>> filters) {
		this.filters = requireNonNull(filters);
		return self();
	}

	@Override
	public final B items(Supplier<Collection<R>> items) {
		this.supplier = requireNonNull(items);
		return self();
	}

	@Override
	public final B validator(Predicate<R> validator) {
		this.validator = requireNonNull(validator);
		return self();
	}

	@Override
	public final B onRefreshException(Consumer<Exception> onRefreshException) {
		this.onRefreshException = requireNonNull(onRefreshException);
		return self();
	}

	@Override
	public final B included(Predicate<R> included) {
		this.included = requireNonNull(included);
		return self();
	}

	@Override
	public final B refresh(boolean refresh) {
		this.refresh = refresh;
		return self();
	}

	@Override
	public final B onSelectionChanged(Runnable listener) {
		selectionListeners.add(requireNonNull(listener));
		return self();
	}

	@Override
	public final B onSelectedItem(Consumer<R> item) {
		itemSelectedListeners.add(requireNonNull(item));
		return self();
	}

	@Override
	public final B onSelectedItems(Consumer<List<R>> items) {
		itemsSelectedListeners.add(requireNonNull(items));
		return self();
	}

	@Override
	public final B onSelectedIndex(Consumer<Integer> index) {
		indexSelectedListeners.add(requireNonNull(index));
		return self();
	}

	@Override
	public final B onSelectedIndexes(Consumer<List<Integer>> indexes) {
		indexesSelectedListeners.add(requireNonNull(indexes));
		return self();
	}

	/**
	 * Builds a {@link FilterTableModel} with the default {@link MultiSelection}, refreshed if {@link #refresh()}.
	 * @return a new {@link FilterTableModel} instance
	 */
	@Override
	public FilterTableModel<R, C> build() {
		FilterTableModel<R, C> model = model(MultiSelection::multiSelection, null);
		if (refresh) {
			model.items().refresh();
		}

		return model;
	}

	/**
	 * Builds a {@link FilterTableModel} with the given {@link MultiSelection} and {@link ItemsListener}, for a
	 * toolkit builder to wrap in its own model. The model is not refreshed, the toolkit model being the one to
	 * refresh once it is in place, see {@link #refresh()}.
	 * @param selection provides the {@link MultiSelection} given the {@link IncludedItems}, such as one
	 * based on a {@code javax.swing.ListSelectionModel}
	 * @param listener notified of fine-grained changes to the included items, for bridging to the toolkit's
	 * own change notifications, such as {@code javax.swing.event.TableModelEvent}s
	 * @return a new {@link FilterTableModel} instance, not refreshed
	 */
	protected final FilterTableModel<R, C> build(Function<IncludedItems<R>, MultiSelection<R>> selection, ItemsListener listener) {
		return model(requireNonNull(selection), requireNonNull(listener));
	}

	/**
	 * @return true if the model should be refreshed once built
	 * @see #refresh(boolean)
	 */
	protected final boolean refresh() {
		return refresh;
	}

	/**
	 * @return this builder instance
	 */
	protected final B self() {
		return (B) this;
	}

	private FilterTableModel<R, C> model(Function<IncludedItems<R>, MultiSelection<R>> selection, @Nullable ItemsListener listener) {
		return new DefaultFilterTableModel<>(this, selection, listener);
	}

	private static <R, C> TableColumns<R, C> validateIdentifiers(TableColumns<R, C> columns) {
		if (new HashSet<>(columns.identifiers()).size() != columns.identifiers().size()) {
			throw new IllegalArgumentException("Column identifiers are not unique");
		}

		return columns;
	}

	private static final class ValidPredicate<R> implements Predicate<R> {

		@Override
		public boolean test(R r) {
			return true;
		}
	}
}
