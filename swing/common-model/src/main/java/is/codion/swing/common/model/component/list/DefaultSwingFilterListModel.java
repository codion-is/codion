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

import is.codion.common.model.component.list.AbstractFilterListModelBuilder;
import is.codion.common.model.component.list.FilterListModel;
import is.codion.common.model.component.list.FilterListSort;
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;


import javax.swing.AbstractListModel;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

/**
 * A Swing {@link javax.swing.ListModel} coat over the UI-agnostic
 * {@link is.codion.common.model.component.list.FilterListModel}: delegates the rich model surface to a
 * common instance — built with a {@link javax.swing.ListSelectionModel} based selection
 * ({@link DefaultListSelection}) — and adds the {@link javax.swing.ListModel} methods, firing
 * {@code ListDataEvent}s off the common model's items.
 */
final class DefaultSwingFilterListModel<T> extends AbstractListModel<T> implements SwingFilterListModel<T> {

	private final FilterListModel<T> model;
	private final FilterListSelection<T> selection;

	private DefaultSwingFilterListModel(DefaultBuilder<T> builder) {
		this.model = builder.model(new ListModelAdapter());
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
			return new DefaultBuilder<>(emptyList());
		}

		@Override
		public <T> Builder<T> items(Collection<T> items) {
			return new DefaultBuilder<>(items);
		}

		@Override
		public <T> Builder<T> items(Supplier<Collection<T>> items) {
			return new DefaultBuilder<>(items);
		}
	}

	static final class DefaultBuilder<T> extends AbstractFilterListModelBuilder<T, Builder<T>> implements Builder<T> {

		static final Builder.ItemsStep ITEMS = new DefaultItemsStep();

		private DefaultBuilder(Collection<T> items) {
			super(items);
		}

		private DefaultBuilder(Supplier<Collection<T>> supplier) {
			super(supplier);
		}

		@Override
		public SwingFilterListModel<T> build() {
			return new DefaultSwingFilterListModel<>(this);
		}

		FilterListModel<T> model(ItemsListener adapter) {
			return build(DefaultListSelection::new, adapter);
		}
	}
}
