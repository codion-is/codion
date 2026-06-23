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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.component.list;

import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.exceptions.Exceptions;

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

final class DefaultFilterListModel<T> implements FilterListModel<T> {

	private final Items<T> items;
	private final MultiSelection<T> selection;
	private final FilterListSort<T> sort;

	DefaultFilterListModel(DefaultBuilder<T> builder) {
		this.sort = new DefaultListSort(builder.comparator);
		Function<Items<T>, Refresher<T>> refresherFactory = builder.refresherFactory != null
						? builder.refresherFactory
						: modelItems -> new DefaultRefresher<>(builder.supplier, modelItems, builder.onRefreshException);
		Function<IncludedItems<T>, MultiSelection<T>> selectionFactory = builder.selectionFactory != null
						? builder.selectionFactory
						: MultiSelection::multiSelection;
		FilterModel.Items.Builder<T> itemsBuilder = FilterModel.Items.<T>builder()
						.refresher(refresherFactory)
						.selection(selectionFactory)
						.sort(sort);
		builder.itemsListeners.forEach(itemsBuilder::listener);
		this.items = itemsBuilder.build();
		this.items.included().predicate().set(builder.included);
		this.selection = (MultiSelection<T>) items.included().selection();
		builder.selectionListeners.forEach(selection.indexes()::addListener);
		builder.itemSelectedListeners.forEach(selection.item()::addConsumer);
		builder.itemsSelectedListeners.forEach(selection.items()::addConsumer);
		builder.indexSelectedListeners.forEach(selection.index()::addConsumer);
		builder.indexesSelectedListeners.forEach(selection.indexes()::addConsumer);
		this.items.set(builder.items);
		this.items.included().sort();
	}

	@Override
	public Items<T> items() {
		return items;
	}

	@Override
	public MultiSelection<T> selection() {
		return selection;
	}

	@Override
	public FilterListSort<T> sort() {
		return sort;
	}

	private enum Order {
		ASCENDING, DESCENDING, UNSORTED
	}

	private final class DefaultListSort implements FilterListSort<T> {

		private final @Nullable Comparator<T> comparator;
		private final Event<Boolean> changed = Event.event();
		private final Value<Order> order = Value.builder()
						.nonNull(Order.ASCENDING)
						.consumer(sortOrder -> changed.accept(sortOrder != Order.UNSORTED))
						.build();

		private DefaultListSort(@Nullable Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(T o1, T o2) {
			if (comparator == null) {
				return 0;
			}
			switch (order.getOrThrow()) {
				case ASCENDING:
					return comparator.compare(o1, o2);
				case DESCENDING:
					return comparator.compare(o2, o1);
				default:
					return 0;
			}
		}

		@Override
		public void ascending() {
			order.set(Order.ASCENDING);
		}

		@Override
		public void descending() {
			order.set(Order.DESCENDING);
		}

		@Override
		public void clear() {
			order.set(Order.UNSORTED);
		}

		@Override
		public boolean sorted() {
			return comparator != null && order.getOrThrow() != Order.UNSORTED;
		}

		@Override
		public Observer<Boolean> observer() {
			return changed.observer();
		}
	}

	private static final class DefaultItemsStep implements Builder.ItemsStep {

		@Override
		public <T> Builder<T> items() {
			return new DefaultBuilder<>(emptyList(), null);
		}

		@Override
		public <T> Builder<T> items(Collection<T> items) {
			return new DefaultBuilder<>(requireNonNull(items), null);
		}

		@Override
		public <T> Builder<T> items(Supplier<Collection<T>> items) {
			return new DefaultBuilder<>(emptyList(), requireNonNull(items));
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		static final Builder.ItemsStep ITEMS = new DefaultItemsStep();

		private final Collection<T> items;
		private final @Nullable Supplier<? extends Collection<T>> supplier;
		private final List<Runnable> selectionListeners = new ArrayList<>();
		private final List<Consumer<T>> itemSelectedListeners = new ArrayList<>();
		private final List<Consumer<List<T>>> itemsSelectedListeners = new ArrayList<>();
		private final List<Consumer<Integer>> indexSelectedListeners = new ArrayList<>();
		private final List<Consumer<List<Integer>>> indexesSelectedListeners = new ArrayList<>();
		private final List<ItemsListener> itemsListeners = new ArrayList<>();

		private @Nullable Comparator<T> comparator;
		private @Nullable Consumer<Exception> onRefreshException;
		private @Nullable Predicate<T> included;
		private @Nullable Function<Items<T>, Refresher<T>> refresherFactory;
		private @Nullable Function<IncludedItems<T>, MultiSelection<T>> selectionFactory;

		private DefaultBuilder(Collection<T> items, @Nullable Supplier<? extends Collection<T>> supplier) {
			this.items = items;
			this.supplier = supplier;
		}

		@Override
		public Builder<T> comparator(@Nullable Comparator<T> comparator) {
			this.comparator = comparator;
			return this;
		}

		@Override
		public Builder<T> onRefreshException(Consumer<Exception> onRefreshException) {
			this.onRefreshException = requireNonNull(onRefreshException);
			return this;
		}

		@Override
		public Builder<T> included(Predicate<T> included) {
			this.included = requireNonNull(included);
			return this;
		}

		@Override
		public Builder<T> onSelectionChanged(Runnable listener) {
			selectionListeners.add(requireNonNull(listener));
			return this;
		}

		@Override
		public Builder<T> onItemSelected(Consumer<T> item) {
			itemSelectedListeners.add(requireNonNull(item));
			return this;
		}

		@Override
		public Builder<T> onItemsSelected(Consumer<List<T>> items) {
			itemsSelectedListeners.add(requireNonNull(items));
			return this;
		}

		@Override
		public Builder<T> onIndexSelected(Consumer<Integer> index) {
			indexSelectedListeners.add(requireNonNull(index));
			return this;
		}

		@Override
		public Builder<T> onIndexesSelected(Consumer<List<Integer>> indexes) {
			indexesSelectedListeners.add(requireNonNull(indexes));
			return this;
		}

		@Override
		public Builder<T> selection(Function<IncludedItems<T>, MultiSelection<T>> selection) {
			this.selectionFactory = requireNonNull(selection);
			return this;
		}

		@Override
		public Builder<T> refresher(Function<Items<T>, Refresher<T>> refresher) {
			this.refresherFactory = requireNonNull(refresher);
			return this;
		}

		@Override
		public Builder<T> listener(ItemsListener itemsListener) {
			itemsListeners.add(requireNonNull(itemsListener));
			return this;
		}

		@Override
		public FilterListModel<T> build() {
			return new DefaultFilterListModel<>(this);
		}
	}

	private static final class DefaultRefresher<R> extends FilterModel.AbstractRefresher<R> {

		private final Items<R> items;
		private final Consumer<Exception> onException;

		private DefaultRefresher(@Nullable Supplier<? extends Collection<R>> supplier, Items<R> items,
		                         @Nullable Consumer<Exception> onException) {
			super((Supplier<Collection<R>>) supplier, false);
			this.items = items;
			this.onException = onException == null ? new RethrowHandler() : onException;
		}

		@Override
		protected boolean isUserInterfaceThread() {
			return false;
		}

		@Override
		protected void refreshAsync(@Nullable Consumer<Collection<R>> onResult) {
			refreshSync(onResult);
		}

		@Override
		protected void refreshSync(@Nullable Consumer<Collection<R>> onResult) {
			items().ifPresent(supplier -> {
				setActive(true);
				try {
					Collection<R> result = supplier.get();
					setActive(false);
					processResult(result);
					if (onResult != null) {
						onResult.accept(result);
					}
					notifyResult(result);
				}
				catch (Exception e) {
					setActive(false);
					onException.accept(e);
				}
			});
		}

		@Override
		protected void processResult(Collection<R> result) {
			items.set(result);
		}

		private static final class RethrowHandler implements Consumer<Exception> {

			@Override
			public void accept(Exception exception) {
				throw Exceptions.runtime(exception);
			}
		}
	}
}
