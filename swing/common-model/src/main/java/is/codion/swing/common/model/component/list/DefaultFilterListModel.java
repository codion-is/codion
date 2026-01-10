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
package is.codion.swing.common.model.component.list;

import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.value.Value;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractListModel;
import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static javax.swing.SortOrder.*;

final class DefaultFilterListModel<T> extends AbstractListModel<T> implements FilterListModel<T> {

	private final Items<T> items;
	private final FilterListSelection<T> selection;
	private final FilterListSort<T> sort;

	DefaultFilterListModel(DefaultBuilder<T> builder) {
		this.sort = new DefaultListSort(builder.comparator);
		this.items = Items.<T>builder()
						.refresher(builder::createRefresher)
						.selection(DefaultListSelection::new)
						.sort(sort)
						.listener(new ListModelAdapter())
						.build();
		this.items.included().predicate().set(builder.included);
		this.selection = (FilterListSelection<T>) items.included().selection();
		builder.selectionListeners.forEach(selection.indexes()::addListener);
		builder.itemSelectedListeners.forEach(selection.item()::addConsumer);
		builder.itemsSelectedListeners.forEach(selection.items()::addConsumer);
		builder.indexSelectedListeners.forEach(selection.index()::addConsumer);
		builder.indexesSelectedListeners.forEach(selection.indexes()::addConsumer);
		builder.selectionConsumers.forEach(selectionConsumer ->
						selectionConsumer.accept(selection));
		this.items.set(builder.items);
		this.items.included().sort();
	}

	@Override
	public Items<T> items() {
		return items;
	}

	@Override
	public FilterListSelection<T> selection() {
		return selection;
	}

	@Override
	public FilterListSort<T> sort() {
		return sort;
	}

	@Override
	public int getSize() {
		return items.included().size();
	}

	@Override
	public T getElementAt(int index) {
		return items.included().get(index);
	}

	private class ListModelAdapter implements ItemsListener {

		@Override
		public void inserted(int firstIndex, int lastIndex) {
			fireIntervalAdded(this, firstIndex, lastIndex);
		}

		@Override
		public void updated(int firstIndex, int lastIndex) {
			fireIntervalRemoved(this, firstIndex, lastIndex);
			fireIntervalAdded(this, firstIndex, lastIndex);
		}

		@Override
		public void deleted(int firstIndex, int lastIndex) {
			fireIntervalRemoved(this, firstIndex, lastIndex);
		}

		@Override
		public void changed() {
			fireContentsChanged(this, 0, getSize());
		}
	}

	private class DefaultListSort implements FilterListSort<T> {

		private final @Nullable Comparator<T> comparator;
		private final Event<Boolean> changed = Event.event();
		private final Value<SortOrder> sortOrder = Value.builder()
						.nonNull(ASCENDING)
						.consumer(order -> changed.accept(order != UNSORTED))
						.build();

		private DefaultListSort(@Nullable Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(T o1, T o2) {
			if (comparator == null) {
				return 0;
			}
			switch (sortOrder.getOrThrow()) {
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
			sortOrder.set(ASCENDING);
		}

		@Override
		public void descending() {
			sortOrder.set(DESCENDING);
		}

		@Override
		public void clear() {
			sortOrder.set(UNSORTED);
		}

		@Override
		public boolean sorted() {
			return comparator != null && sortOrder.getOrThrow() != UNSORTED;
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
		private final List<Consumer<FilterListSelection<T>>> selectionConsumers = new ArrayList<>();

		private @Nullable Comparator<T> comparator;
		private boolean async = ASYNC.getOrThrow();
		private @Nullable Consumer<Exception> onRefreshException;
		private @Nullable Predicate<T> included;

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
		public Builder<T> async(boolean async) {
			this.async = async;
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
		public Builder<T> selection(Consumer<FilterListSelection<T>> selection) {
			selectionConsumers.add(requireNonNull(selection));
			return this;
		}

		@Override
		public FilterListModel<T> build() {
			return new DefaultFilterListModel<>(this);
		}

		private Refresher<T> createRefresher(Items<T> items) {
			return new DefaultRefreshWorker<>(supplier, items, async, onRefreshException);
		}

		private static final class DefaultRefreshWorker<R> extends AbstractRefreshWorker<R> {

			private final Items<R> items;

			private DefaultRefreshWorker(@Nullable Supplier<? extends Collection<R>> supplier,
																	 Items<R> items, boolean async,
																	 @Nullable Consumer<Exception> onException) {
				super((Supplier<Collection<R>>) supplier, async, onException);
				this.items = items;
			}

			@Override
			protected void processResult(Collection<R> result) {
				items.set(result);
			}
		}
	}
}
