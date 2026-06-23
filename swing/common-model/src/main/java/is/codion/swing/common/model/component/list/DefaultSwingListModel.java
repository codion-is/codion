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

import is.codion.common.model.component.list.FilterListModel;
import is.codion.common.model.component.list.FilterListSort;
import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractListModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A Swing {@link javax.swing.ListModel} coat over the UI-agnostic
 * {@link is.codion.common.model.component.list.FilterListModel}: delegates the rich model surface to a
 * common instance — built with a {@link javax.swing.ListSelectionModel} based selection
 * ({@link DefaultListSelection}) and a {@code ProgressWorker} based refresher — and adds the
 * {@link javax.swing.ListModel} methods, firing {@code ListDataEvent}s off the common model's items.
 */
final class DefaultSwingListModel<T> extends AbstractListModel<T> implements SwingListModel<T> {

	private final FilterListModel<T> model;
	private final FilterListSelection<T> selection;

	private DefaultSwingListModel(DefaultBuilder<T> builder) {
		this.model = builder.builder
						.selection(DefaultListSelection::new)
						.refresher(modelItems -> new ListRefreshWorker<>(builder.supplier, builder.async, builder.onRefreshException, modelItems))
						.listener(new ListModelAdapter())
						.build();
		this.selection = (FilterListSelection<T>) model.selection();
	}

	@Override
	public Items<T> items() {
		return model.items();
	}

	@Override
	public FilterListSelection<T> selection() {
		return selection;
	}

	@Override
	public FilterListSort<T> sort() {
		return model.sort();
	}

	@Override
	public int getSize() {
		// Guarded: the common model fires through ListModelAdapter during its own construction
		// (initial items.set), before the model field below has been assigned.
		return model == null ? 0 : model.items().included().size();
	}

	@Override
	public T getElementAt(int index) {
		return model.items().included().get(index);
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

	private static final class DefaultItemsStep implements Builder.ItemsStep {

		@Override
		public <T> Builder<T> items() {
			return new DefaultBuilder<>(FilterListModel.builder().items(), null);
		}

		@Override
		public <T> Builder<T> items(Collection<T> items) {
			return new DefaultBuilder<>(FilterListModel.builder().items(items), null);
		}

		@Override
		public <T> Builder<T> items(Supplier<Collection<T>> items) {
			return new DefaultBuilder<>(FilterListModel.builder().items(items), items);
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		static final Builder.ItemsStep ITEMS = new DefaultItemsStep();

		private final FilterListModel.Builder<T> builder;
		private final @Nullable Supplier<Collection<T>> supplier;

		private boolean async = FilterModel.ASYNC.getOrThrow();
		private @Nullable Consumer<Exception> onRefreshException;

		private DefaultBuilder(FilterListModel.Builder<T> builder, @Nullable Supplier<Collection<T>> supplier) {
			this.builder = builder;
			this.supplier = supplier;
		}

		@Override
		public Builder<T> comparator(@Nullable Comparator<T> comparator) {
			builder.comparator(comparator);
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
			builder.included(included);
			return this;
		}

		@Override
		public Builder<T> onSelectionChanged(Runnable listener) {
			builder.onSelectionChanged(listener);
			return this;
		}

		@Override
		public Builder<T> onItemSelected(Consumer<T> item) {
			builder.onItemSelected(item);
			return this;
		}

		@Override
		public Builder<T> onItemsSelected(Consumer<List<T>> items) {
			builder.onItemsSelected(items);
			return this;
		}

		@Override
		public Builder<T> onIndexSelected(Consumer<Integer> index) {
			builder.onIndexSelected(index);
			return this;
		}

		@Override
		public Builder<T> onIndexesSelected(Consumer<List<Integer>> indexes) {
			builder.onIndexesSelected(indexes);
			return this;
		}

		@Override
		public SwingListModel<T> build() {
			return new DefaultSwingListModel<>(this);
		}
	}

	private static final class ListRefreshWorker<T> extends AbstractRefreshWorker<T> {

		private final Items<T> items;

		private ListRefreshWorker(@Nullable Supplier<Collection<T>> supplier, boolean async,
															@Nullable Consumer<Exception> onException, Items<T> items) {
			super(supplier, async, onException);
			this.items = items;
		}

		@Override
		protected void processResult(Collection<T> result) {
			items.set(result);
		}
	}
}
