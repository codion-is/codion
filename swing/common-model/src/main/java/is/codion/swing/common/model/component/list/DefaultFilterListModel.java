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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.list;

import is.codion.common.event.Event;
import is.codion.common.model.filter.FilterModel.VisibleItems.ItemsListener;
import is.codion.common.observable.Observer;
import is.codion.common.value.Value;

import javax.swing.AbstractListModel;
import javax.swing.SortOrder;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static javax.swing.SortOrder.*;

final class DefaultFilterListModel<T> extends AbstractListModel<T> implements FilterListModel<T> {

	private final Items<T> items;
	private final FilterListSelection<T> selection;
	private final FilterListSort<T> sort;

	DefaultFilterListModel(DefaultBuilder<T> builder) {
		this.sort = new DefaultListSort(builder.comparator);
		this.items = Items.<T>builder(builder::createRefresher)
						.selection(DefaultListSelection::new)
						.sort(sort)
						.listener(new ListModelAdapter())
						.build();
		this.items.visible().predicate().set(builder.visiblePredicate);
		this.selection = (FilterListSelection<T>) items.visible().selection();
		this.items.set(builder.items);
		this.items.visible().sort();
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
		return items.visible().count();
	}

	@Override
	public T getElementAt(int index) {
		return items.visible().get(index);
	}

	private class ListModelAdapter implements ItemsListener {

		@Override
		public void inserted(int firstIndex, int lastIndex) {
			fireIntervalAdded(this, firstIndex, lastIndex);
		}

		@Override
		public void updated(int firstIndex, int lastIndex) {
			fireIntervalRemoved(this, firstIndex, lastIndex);
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

		private final Comparator<T> comparator;
		private final Event<Boolean> changed = Event.event();
		private final Value<SortOrder> sortOrder = Value.builder()
						.nonNull(ASCENDING)
						.consumer(order -> changed.accept(order != UNSORTED))
						.build();

		private DefaultListSort(Comparator<T> comparator) {
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

	static final class DefaultBuilder<T> implements Builder<T> {

		private final Collection<T> items;

		private Supplier<? extends Collection<T>> supplier;
		private Comparator<T> comparator;
		private boolean asyncRefresh = ASYNC_REFRESH.getOrThrow();
		private Predicate<T> visiblePredicate;

		DefaultBuilder(Collection<T> items) {
			this.items = items;
		}

		@Override
		public Builder<T> supplier(Supplier<? extends Collection<T>> supplier) {
			this.supplier = requireNonNull(supplier);
			return this;
		}

		@Override
		public Builder<T> comparator(Comparator<T> comparator) {
			this.comparator = comparator;
			return this;
		}

		@Override
		public Builder<T> asyncRefresh(boolean asyncRefresh) {
			this.asyncRefresh = asyncRefresh;
			return this;
		}

		@Override
		public Builder<T> visible(Predicate<T> predicate) {
			this.visiblePredicate = requireNonNull(predicate);
			return this;
		}

		@Override
		public FilterListModel<T> build() {
			return new DefaultFilterListModel<>(this);
		}

		private Refresher<T> createRefresher(Items<T> items) {
			return new DefaultRefreshWorker<>(supplier, items, asyncRefresh);
		}

		private static final class DefaultRefreshWorker<R> extends AbstractRefreshWorker<R> {

			private final Items<R> items;

			private DefaultRefreshWorker(Supplier<? extends Collection<R>> supplier,
																	 Items<R> items, boolean asyncRefresh) {
				super((Supplier<Collection<R>>) supplier, asyncRefresh);
				this.items = items;
			}

			@Override
			protected void processResult(Collection<R> result) {
				items.set(result);
			}
		}
	}
}
