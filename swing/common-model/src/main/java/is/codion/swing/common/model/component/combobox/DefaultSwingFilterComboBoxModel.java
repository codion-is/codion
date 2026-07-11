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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.model.component.combobox.FilterComboBoxModel;
import is.codion.common.model.selection.SingleSelection;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.item.Item;

import org.jspecify.annotations.Nullable;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A Swing {@link javax.swing.ComboBoxModel} coat over the UI-agnostic
 * {@link FilterComboBoxModel}: delegates the rich model surface to a
 * common instance and adds the {@link javax.swing.ListModel}/{@code ComboBoxModel} methods, firing
 * {@link ListDataEvent}s off the common model's own observable items + selection.
 */
final class DefaultSwingFilterComboBoxModel<T> implements SwingFilterComboBoxModel<T> {

	/**
	 * Due to a java.util.ConcurrentModificationException in OSX
	 */
	private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

	private final FilterComboBoxModel<T> model;

	private DefaultSwingFilterComboBoxModel(FilterComboBoxModel<T> model) {
		this.model = model;
		// Bridge the common model's observables to Swing list-data events.
		model.items().included().observer().addListener(this::fireContentsChanged);
		model.selection().item().addListener(this::fireSelectionChanged);
	}

	static <T> SwingFilterComboBoxModel<T> model(FilterComboBoxModel<T> model) {
		return new DefaultSwingFilterComboBoxModel<>(model);
	}

	@Override
	public ComboBoxItems<T> items() {
		return model.items();
	}

	@Override
	public Sort<T> sort() {
		return model.sort();
	}

	@Override
	public SingleSelection<T> selection() {
		return model.selection();
	}

	@Override
	public @Nullable T selectedItem() {
		return model.selectedItem();
	}

	@Override
	public <V> Value<V> selector(ItemFinder<T, V> itemFinder) {
		return model.selector(itemFinder);
	}

	@Override
	public @Nullable Object getSelectedItem() {
		return selectedItem();
	}

	@Override
	public void setSelectedItem(@Nullable Object item) {
		// The common selection applies the translator + null-item handling via its Object-aware setValue.
		model.selection().item().set((T) item);
	}

	@Override
	public int getSize() {
		ComboBoxItems<T> items = model.items();
		return items.included().size() + (items.includesNull() ? 1 : 0);
	}

	@Override
	public @Nullable T getElementAt(int index) {
		ComboBoxItems<T> items = model.items();
		if (items.includesNull() && index == 0) {
			return items.nullItem();
		}
		//get(int) indexes the backing list, which holds the null item at index 0 when includesNull, so no offset here
		return items.included().get(index);
	}

	@Override
	public void addListDataListener(ListDataListener listener) {
		listDataListeners.add(requireNonNull(listener));
	}

	@Override
	public void removeListDataListener(ListDataListener listener) {
		listDataListeners.remove(requireNonNull(listener));
	}

	private void fireContentsChanged() {
		fireListDataEvent(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE));
	}

	private void fireSelectionChanged() {
		//the Swing convention for a combo box selection change is (-1, -1), not a full-list invalidation
		fireListDataEvent(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1));
	}

	private void fireListDataEvent(ListDataEvent event) {
		for (ListDataListener dataListener : listDataListeners) {
			dataListener.contentsChanged(event);
		}
	}

	private static final class DefaultItemsStep implements Builder.ItemsStep {

		@Override
		public <T> Builder<T> items(Collection<T> items) {
			return new DefaultBuilder<>(FilterComboBoxModel.builder().items(items));
		}

		@Override
		public <T> Builder<T> items(Supplier<Collection<T>> items) {
			return new DefaultBuilder<>(FilterComboBoxModel.builder().items(items));
		}

		@Override
		public <T> SwingItemComboBoxModelBuilder<T> items(List<Item<T>> items) {
			return new DefaultItemComboBoxModelBuilder<>(FilterComboBoxModel.builder().items(items));
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		static final DefaultItemsStep ITEMS = new DefaultItemsStep();

		private final FilterComboBoxModel.Builder<T> builder;

		private DefaultBuilder(FilterComboBoxModel.Builder<T> builder) {
			this.builder = builder;
		}

		@Override
		public Builder<T> comparator(@Nullable Comparator<T> comparator) {
			builder.comparator(comparator);
			return this;
		}

		@Override
		public Builder<T> includeNull(boolean includeNull) {
			builder.includeNull(includeNull);
			return this;
		}

		@Override
		public Builder<T> nullItem(@Nullable T nullItem) {
			builder.nullItem(nullItem);
			return this;
		}

		@Override
		public Builder<T> select(@Nullable T item) {
			builder.select(item);
			return this;
		}

		@Override
		public Builder<T> translator(Function<Object, T> translator) {
			builder.translator(translator);
			return this;
		}

		@Override
		public Builder<T> filterSelected(boolean filterSelected) {
			builder.filterSelected(filterSelected);
			return this;
		}

		@Override
		public Builder<T> onItemSelected(Consumer<@Nullable T> item) {
			builder.onItemSelected(item);
			return this;
		}

		@Override
		public Builder<T> onRefreshException(Consumer<Exception> onRefreshException) {
			builder.onRefreshException(requireNonNull(onRefreshException));
			return this;
		}

		@Override
		public Builder<T> refresh(boolean refresh) {
			builder.refresh(refresh);
			return this;
		}

		@Override
		public SwingFilterComboBoxModel<T> build() {
			return new DefaultSwingFilterComboBoxModel<>(builder.build());
		}
	}

	static final class DefaultItemComboBoxModelBuilder<T> implements SwingItemComboBoxModelBuilder<T> {

		private final ItemComboBoxModelBuilder<T> builder;

		private DefaultItemComboBoxModelBuilder(ItemComboBoxModelBuilder<T> builder) {
			this.builder = builder;
		}

		@Override
		public SwingItemComboBoxModelBuilder<T> sorted(boolean sorted) {
			builder.sorted(sorted);
			return this;
		}

		@Override
		public SwingItemComboBoxModelBuilder<T> sorted(Comparator<Item<T>> comparator) {
			builder.sorted(comparator);
			return this;
		}

		@Override
		public SwingItemComboBoxModelBuilder<T> selected(@Nullable T selected) {
			builder.selected(selected);
			return this;
		}

		@Override
		public SwingItemComboBoxModelBuilder<T> selected(Item<T> selected) {
			builder.selected(selected);
			return this;
		}

		@Override
		public SwingFilterComboBoxModel<Item<T>> build() {
			return new DefaultSwingFilterComboBoxModel<>(builder.build());
		}
	}
}
