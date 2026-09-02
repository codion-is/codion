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
package is.codion.common.model.component.list;

import is.codion.common.model.filter.FilterModel.IncludedItems;
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.common.model.selection.MultiSelection;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A base class for {@link FilterListModel.Builder} implementations. A toolkit builder extends this class, adding
 * its own options and overriding {@link #build()} to wrap the {@link FilterListModel} built by
 * {@link #build(Function, ItemsListener)} in its own model.
 * @param <T> the item type
 * @param <B> the builder type
 * @see #build(Function, ItemsListener)
 */
public abstract class AbstractFilterListModelBuilder<T, B extends FilterListModel.Builder<T, B>>
				implements FilterListModel.Builder<T, B> {

	final Collection<T> items;
	final @Nullable Supplier<Collection<T>> supplier;
	final List<Runnable> selectionListeners = new ArrayList<>();
	final List<Consumer<T>> itemSelectedListeners = new ArrayList<>();
	final List<Consumer<List<T>>> itemsSelectedListeners = new ArrayList<>();
	final List<Consumer<Integer>> indexSelectedListeners = new ArrayList<>();
	final List<Consumer<List<Integer>>> indexesSelectedListeners = new ArrayList<>();

	@Nullable Comparator<T> comparator;
	@Nullable Consumer<Exception> onRefreshException;
	@Nullable Predicate<T> included;

	/**
	 * @param items the items
	 */
	protected AbstractFilterListModelBuilder(Collection<T> items) {
		this.items = requireNonNull(items);
		this.supplier = null;
	}

	/**
	 * @param supplier supplies the items
	 */
	protected AbstractFilterListModelBuilder(Supplier<Collection<T>> supplier) {
		this.items = emptyList();
		this.supplier = requireNonNull(supplier);
	}

	@Override
	public final B comparator(@Nullable Comparator<T> comparator) {
		this.comparator = comparator;
		return self();
	}

	@Override
	public final B onRefreshException(Consumer<Exception> onRefreshException) {
		this.onRefreshException = requireNonNull(onRefreshException);
		return self();
	}

	@Override
	public final B included(Predicate<T> included) {
		this.included = requireNonNull(included);
		return self();
	}

	@Override
	public final B onSelectionChanged(Runnable listener) {
		selectionListeners.add(requireNonNull(listener));
		return self();
	}

	@Override
	public final B onSelectedItem(Consumer<T> item) {
		itemSelectedListeners.add(requireNonNull(item));
		return self();
	}

	@Override
	public final B onSelectedItems(Consumer<List<T>> items) {
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
	 * Builds a {@link FilterListModel} with the default {@link MultiSelection}.
	 * @return a new {@link FilterListModel} instance
	 */
	@Override
	public FilterListModel<T> build() {
		return model(MultiSelection::multiSelection, null);
	}

	/**
	 * Builds a {@link FilterListModel} with the given {@link MultiSelection} and {@link ItemsListener}, for a
	 * toolkit builder to wrap in its own model.
	 * @param selection provides the {@link MultiSelection} given the {@link IncludedItems}, such as one
	 * based on a {@code javax.swing.ListSelectionModel}
	 * @param listener notified of fine-grained changes to the included items, for bridging to the toolkit's
	 * own change notifications, such as {@code javax.swing.event.ListDataEvent}s
	 * @return a new {@link FilterListModel} instance
	 */
	protected final FilterListModel<T> build(Function<IncludedItems<T>, MultiSelection<T>> selection, ItemsListener listener) {
		return model(requireNonNull(selection), requireNonNull(listener));
	}

	/**
	 * @return this builder instance
	 */
	protected final B self() {
		return (B) this;
	}

	private FilterListModel<T> model(Function<IncludedItems<T>, MultiSelection<T>> selection, @Nullable ItemsListener listener) {
		return new DefaultFilterListModel<>(this, selection, listener);
	}
}
