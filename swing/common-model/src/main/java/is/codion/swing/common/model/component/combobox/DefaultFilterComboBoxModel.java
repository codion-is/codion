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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.item.Item;
import is.codion.common.model.selection.SingleSelection;
import is.codion.common.observable.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.AbstractFilterModelRefresher;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.value.Value.Notify.WHEN_SET;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

final class DefaultFilterComboBoxModel<T> implements FilterComboBoxModel<T> {

	private static final Function<Object, ?> DEFAULT_SELECTED_ITEM_TRANSLATOR = new DefaultSelectedItemTranslator<>();
	private static final Comparator<?> DEFAULT_COMPARATOR = new DefaultComparator<>();
	private static final Comparator<?> NULL_COMPARATOR = new NullComparator<>();

	private final DefaultComboBoxSelection selection;
	private final DefaultComboBoxItems modelItems;

	/**
	 * Due to a java.util.ConcurrentModificationException in OSX
	 */
	private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

	private DefaultFilterComboBoxModel(DefaultBuilder<T> builder) {
		selection = new DefaultComboBoxSelection(builder.translator);
		modelItems = new DefaultComboBoxItems(builder.includeNull, builder.nullItem, builder.comparator, builder.supplier, builder.items);
	}

	@Override
	public ComboBoxItems<T> items() {
		return modelItems;
	}

	@Override
	public ComboBoxSelection<T> selection() {
		return selection;
	}

	@Override
	public T getSelectedItem() {
		if (selection.selected.item == null) {
			return modelItems.nullItem;
		}

		return selection.selected.item;
	}

	@Override
	public void setSelectedItem(Object item) {
		selection.selected.setSelectedItem(item);
	}

	@Override
	public void addListDataListener(ListDataListener listener) {
		listDataListeners.add(requireNonNull(listener));
	}

	@Override
	public void removeListDataListener(ListDataListener listener) {
		listDataListeners.remove(requireNonNull(listener));
	}

	@Override
	public T getElementAt(int index) {
		T element = modelItems.visible.items.get(index);
		if (element == null) {
			return modelItems.nullItem;
		}

		return element;
	}

	@Override
	public int getSize() {
		return modelItems.visible.items.size();
	}

	@Override
	public <V> Value<V> createSelectorValue(ItemFinder<T, V> itemFinder) {
		return new SelectorValue<>(itemFinder);
	}

	private void fireContentsChanged() {
		ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
		for (ListDataListener dataListener : listDataListeners) {
			dataListener.contentsChanged(event);
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		private final Collection<T> items;
		private final Supplier<Collection<T>> supplier;

		private Comparator<T> comparator = (Comparator<T>) DEFAULT_COMPARATOR;
		private Function<Object, T> translator = (Function<Object, T>) DEFAULT_SELECTED_ITEM_TRANSLATOR;
		private boolean includeNull;
		private T nullItem;

		DefaultBuilder(Collection<T> items, Supplier<Collection<T>> supplier) {
			this.items = items;
			this.supplier = supplier;
		}

		@Override
		public Builder<T> comparator(Comparator<T> comparator) {
			this.comparator = comparator == null ? (Comparator<T>) NULL_COMPARATOR : comparator;
			return this;
		}

		@Override
		public Builder<T> includeNull(boolean includeNull) {
			this.includeNull = includeNull;
			return this;
		}

		@Override
		public Builder<T> nullItem(T nullItem) {
			this.nullItem = nullItem;

			return includeNull(nullItem != null);
		}

		@Override
		public Builder<T> translator(Function<Object, T> translator) {
			this.translator = requireNonNull(translator);
			return this;
		}

		@Override
		public FilterComboBoxModel<T> build() {
			return new DefaultFilterComboBoxModel<>(this);
		}
	}

	static final class DefaultItemComboBoxModelBuilder<T> implements ItemComboBoxModelBuilder<T> {

		private final List<Item<T>> items;
		private final Item<T> nullItem;

		private boolean sorted = false;
		private Comparator<Item<T>> comparator;
		private Item<T> selected;

		DefaultItemComboBoxModelBuilder(List<Item<T>> modelItems) {
			items = new ArrayList<>(requireNonNull(modelItems));
			int indexOfNullItem = items.indexOf(Item.item(null));
			nullItem = indexOfNullItem >= 0 ? items.remove(indexOfNullItem) : null;
		}

		@Override
		public ItemComboBoxModelBuilder<T> sorted(boolean sorted) {
			this.sorted = sorted;
			if (!sorted) {
				this.comparator = null;
			}
			return this;
		}

		@Override
		public ItemComboBoxModelBuilder<T> sorted(Comparator<Item<T>> comparator) {
			this.sorted = true;
			this.comparator = requireNonNull(comparator);
			return this;
		}

		@Override
		public ItemComboBoxModelBuilder<T> selected(T selected) {
			return selected(Item.item(selected));
		}

		@Override
		public ItemComboBoxModelBuilder<T> selected(Item<T> selected) {
			if (!items.contains(requireNonNull(selected))) {
				throw new IllegalArgumentException("Model does not contain item: " + selected);
			}
			this.selected = requireNonNull(selected);
			return this;
		}

		@Override
		public FilterComboBoxModel<Item<T>> build() {
			FilterComboBoxModel.Builder<Item<T>> builder = new DefaultBuilder<>(items, null)
							.translator(new SelectedItemTranslator<>(items))
							.nullItem(nullItem);
			if (!sorted) {
				builder.comparator(null);
			}
			if (comparator != null) {
				builder.comparator(comparator);
			}
			FilterComboBoxModel<Item<T>> comboBoxModel = builder.build();
			if (selected != null) {
				comboBoxModel.selection().item().set(selected);
			}

			return comboBoxModel;
		}
	}

	private final class DefaultComboBoxItems implements ComboBoxItems<T> {

		private final Lock lock = new Lock() {};

		private final DefaultRefresher refresher;
		private final DefaultVisibleItems visible;
		private final DefaultFilteredItems filtered = new DefaultFilteredItems();

		private final boolean includeNull;
		private final T nullItem;

		private boolean cleared = true;

		private DefaultComboBoxItems(boolean includeNull, T nullItem, Comparator<T> comparator,
																 Supplier<Collection<T>> supplier, Collection<T> items) {
			this.includeNull = includeNull;
			this.nullItem = nullItem;
			this.visible = new DefaultVisibleItems(comparator);
			if (includeNull) {
				visible.items.add(null);
			}
			visible.predicate.addListener(this::filter);
			if (supplier == null) {
				refresher = new DefaultRefresher(this::get);
				if (items != null) {
					set(items);
				}
			}
			else {
				refresher = new DefaultRefresher(supplier);
			}
		}

		@Override
		public Refresher<T> refresher() {
			return refresher;
		}

		@Override
		public void refresh() {
			refresher.doRefresh(null);
		}

		@Override
		public void refresh(Consumer<Collection<T>> onResult) {
			refresher.doRefresh(requireNonNull(onResult));
		}

		@Override
		public Collection<T> get() {
			synchronized (lock) {
				List<T> visibleItems = visible.get();
				if (filtered.items.isEmpty()) {
					return unmodifiableCollection(new ArrayList<>(visibleItems));
				}
				List<T> entities = new ArrayList<>(visibleItems.size() + filtered.items.size());
				entities.addAll(visibleItems);
				entities.addAll(filtered.items);

				return unmodifiableList(entities);
			}
		}

		@Override
		public void set(Collection<T> items) {
			requireNonNull(items);
			synchronized (lock) {
				filtered.items.clear();
				visible.items.clear();
				if (includeNull) {
					visible.items.add(0, null);
				}
				//remove duplicates while preserving the original order
				visible.items.addAll(new LinkedHashSet<>(items));
				filterInternal();
				replaceSelectedItem();
				cleared = items.isEmpty();
				visible.sortInternal();
				filtered.notifyChanges();
				visible.notifyChanges();
			}
		}

		@Override
		public void add(T item) {
			requireNonNull(item);
			synchronized (lock) {
				if (visible.predicate.isNull() || visible.predicate.getOrThrow().test(item)) {
					if (!visible.items.contains(item)) {
						visible.items.add(item);
						visible.sortInternal();
						visible.notifyChanges();
					}
				}
				else if (!filtered.items.contains(item)) {
					filtered.items.add(item);
					filtered.notifyChanges();
				}
			}
		}

		@Override
		public void add(Collection<T> items) {
			synchronized (lock) {
				for (T item : requireNonNull(items)) {
					add(item);
				}
			}
		}

		@Override
		public void remove(T item) {
			requireNonNull(item);
			synchronized (lock) {
				if (filtered.items.remove(item)) {
					filtered.notifyChanges();
				}
				else if (visible.items.remove(item)) {
					visible.notifyChanges();
					updateSelectedItem(item);
				}
			}
		}

		@Override
		public void remove(Collection<T> items) {
			synchronized (lock) {
				for (T item : requireNonNull(items)) {
					remove(item);
				}
			}
		}

		@Override
		public VisibleItems<T> visible() {
			return visible;
		}

		@Override
		public FilteredItems<T> filtered() {
			return filtered;
		}

		@Override
		public boolean contains(T item) {
			synchronized (lock) {
				return visible.items.contains(item) || filtered.items.contains(item);
			}
		}

		@Override
		public int count() {
			synchronized (lock) {
				return visible.count() + filtered.count();
			}
		}

		@Override
		public void filter() {
			synchronized (lock) {
				if (count() > 0) {
					filter(selection.filterSelected.get());
				}
			}
		}

		@Override
		public void clear() {
			selection.item().clear();
			synchronized (lock) {
				set(emptyList());
			}
		}

		@Override
		public void replace(T item, T replacement) {
			requireNonNull(item);
			requireNonNull(replacement);
			synchronized (lock) {
				remove(item);
				add(replacement);
			}
			if (Objects.equals(selection.selected.item, item)) {
				selection.selected.replaceWith(item);
			}
		}

		@Override
		public boolean cleared() {
			return cleared;
		}

		private void filter(boolean filterSelectedItem) {
			visible.items.addAll(filtered.items);
			filtered.items.clear();
			filterInternal();
			visible.sortInternal();
			if (filterSelectedItem
							&& selection.selected.item != null
							&& !visible.items.contains(selection.selected.item)) {
				selection.selected.setSelectedItem(null);
			}
			filtered.notifyChanges();
			visible.notifyChanges();
		}

		private void filterInternal() {
			Predicate<T> predicate = visible.predicate.get();
			if (predicate != null) {
				for (Iterator<T> iterator = visible.items.listIterator(); iterator.hasNext(); ) {
					T item = iterator.next();
					if (item != null && !predicate.test(item)) {
						filtered.items.add(item);
						iterator.remove();
					}
				}
			}
		}

		private void updateSelectedItem(T removedItem) {
			if (Objects.equals(selection.selected.item, removedItem)) {
				if (modelItems.nullItem != null) {
					setSelectedItem(null);
				}
				else if (!modelItems.visible.items.isEmpty()) {
					setSelectedItem(modelItems.visible.items.get(0));
				}
			}
		}

		private void replaceSelectedItem() {
			if (selection.selected.item != null) {
				int index = visible.items.indexOf(selection.selected.item);
				if (index != -1) {
					//update the selected item since the underlying data could have changed
					selection.selected.item = visible.items.get(index);
				}
			}
		}

		private final class DefaultVisibleItems implements VisibleItems<T> {

			private final Value<Predicate<T>> predicate = Value.builder()
							.<Predicate<T>>nullable()
							.notify(WHEN_SET)
							.build();
			private final Comparator<T> comparator;
			private final List<T> items = new ArrayList<>();
			private final Event<List<T>> event = Event.event();

			private DefaultVisibleItems(Comparator<T> comparator) {
				this.comparator = requireNonNull(comparator);
			}

			@Override
			public Value<Predicate<T>> predicate() {
				return predicate;
			}

			@Override
			public List<T> get() {
				synchronized (lock) {
					if (items.isEmpty()) {
						return emptyList();
					}
					if (!includeNull) {
						return unmodifiableList(items);
					}

					return unmodifiableList(items.subList(1, items.size()));
				}
			}

			@Override
			public Observer<List<T>> observer() {
				return event.observer();
			}

			@Override
			public boolean contains(T item) {
				if (item == null) {
					return includeNull;
				}
				synchronized (lock) {
					return items.contains(item);
				}
			}

			@Override
			public int indexOf(T item) {
				synchronized (lock) {
					return items.indexOf(item);
				}
			}

			@Override
			public T get(int index) {
				synchronized (lock) {
					return items.get(index);
				}
			}

			@Override
			public boolean add(int index, T item) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean add(int index, Collection<T> items) {
				throw new UnsupportedOperationException();
			}

			@Override
			public T remove(int index) {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<T> remove(int fromIndex, int toIndex) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean set(int index, T item) {
				throw new UnsupportedOperationException();
			}

			@Override
			public int count() {
				synchronized (lock) {
					if (items.isEmpty()) {
						return 0;
					}
					if (!includeNull) {
						return items.size();
					}

					return items.size() - 1;
				}
			}

			@Override
			public Comparator<T> comparator() {
				return comparator;
			}

			@Override
			public void sort() {
				synchronized (lock) {
					if (sortInternal()) {
						notifyChanges();
					}
				}
			}

			private boolean sortInternal() {
				if (comparator != NULL_COMPARATOR && count() > 0) {
					items.subList(includeNull ? 1 : 0, items.size()).sort(comparator);
					return true;
				}

				return false;
			}

			private void notifyChanges() {
				fireContentsChanged();
				event.accept(get());
			}
		}

		private final class DefaultFilteredItems implements FilteredItems<T> {

			private final List<T> items = new ArrayList<>();
			private final Event<Collection<T>> event = Event.event();

			@Override
			public Collection<T> get() {
				synchronized (lock) {
					return unmodifiableCollection(items);
				}
			}

			@Override
			public Observer<Collection<T>> observer() {
				return event.observer();
			}

			@Override
			public boolean contains(T item) {
				synchronized (lock) {
					return items.contains(item);
				}
			}

			@Override
			public int count() {
				synchronized (lock) {
					return items.size();
				}
			}

			private void notifyChanges() {
				event.accept(get());
			}
		}
	}

	private final class DefaultComboBoxSelection implements ComboBoxSelection<T> {

		private final SelectedItem selected;
		private final State filterSelected = State.state(false);

		private DefaultComboBoxSelection(Function<Object, T> translator) {
			selected = new SelectedItem(translator);
		}

		@Override
		public ObservableState empty() {
			return selected.empty.observable();
		}

		@Override
		public Observer<?> changing() {
			return selected.changing;
		}

		@Override
		public Item<T> item() {
			return selected;
		}

		@Override
		public void clear() {
			selected.clear();
		}

		@Override
		public State filterSelected() {
			return filterSelected;
		}
	}

	private final class SelectedItem implements SingleSelection.Item<T> {

		private final Event<T> changing = Event.event();
		private final Event<T> event = Event.event();
		private final State empty = State.state(true);
		private final Function<Object, T> translator;

		private T item = null;

		private SelectedItem(Function<Object, T> translator) {
			this.translator = translator;
			event.addListener(DefaultFilterComboBoxModel.this::fireContentsChanged);
		}

		@Override
		public T get() {
			return item;
		}

		@Override
		public void set(T item) {
			setSelectedItem(item);
		}

		@Override
		public void clear() {
			setSelectedItem(null);
		}

		@Override
		public Observer<T> observer() {
			return event.observer();
		}

		private void setSelectedItem(Object item) {
			T toSelect = translator.apply(Objects.equals(modelItems.nullItem, item) ? null : item);
			if (!Objects.equals(this.item, toSelect)) {
				changing.accept(toSelect);
				this.item = toSelect;
				empty.set(toSelect == null);
				event.accept(toSelect);
			}
		}

		private void replaceWith(T replacement) {
			selection.selected.item = selection.selected.translator.apply(null);
			setSelectedItem(replacement);
		}
	}

	private final class SelectorValue<V> extends AbstractValue<V> {

		private final ItemFinder<T, V> itemFinder;

		private SelectorValue(ItemFinder<T, V> itemFinder) {
			this.itemFinder = requireNonNull(itemFinder);
			selection.selected.event.addListener(this::notifyListeners);
		}

		@Override
		protected V getValue() {
			if (selection.selected.empty.get()) {
				return null;
			}

			return itemFinder.value(selection.item().get());
		}

		@Override
		protected void setValue(V value) {
			setSelectedItem(value == null ? null : itemFinder.findItem(modelItems.visible.get(), value).orElse(null));
		}
	}

	private final class DefaultRefresher extends AbstractFilterModelRefresher<T> {

		private DefaultRefresher(Supplier<Collection<T>> supplier) {
			super(supplier);
		}

		@Override
		protected void processResult(Collection<T> result) {
			modelItems.set(result);
		}

		private void doRefresh(Consumer<Collection<T>> onRefresh) {
			super.refresh(onRefresh);
		}
	}

	private static final class DefaultSelectedItemTranslator<T> implements Function<Object, T> {

		@Override
		public T apply(Object item) {
			return (T) item;
		}
	}

	private static final class SelectedItemTranslator<T> implements Function<Object, Item<T>> {

		private final Map<T, Item<T>> itemMap;

		private SelectedItemTranslator(List<Item<T>> items) {
			itemMap = items.stream()
							.collect(toMap(Item::value, Function.identity()));
		}

		@Override
		public Item<T> apply(Object item) {
			if (item instanceof Item) {
				return itemMap.get(((Item<T>) item).value());
			}

			return itemMap.get(item);
		}
	}

	private static final class NullComparator<T> implements Comparator<T> {

		@Override
		public int compare(T o1, T o2) {
			return 0;
		}
	}

	private static final class DefaultComparator<T> implements Comparator<T> {

		private final Comparator<T> collator = Text.collator();

		@Override
		public int compare(T o1, T o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return -1;
			}
			if (o2 == null) {
				return 1;
			}
			if (o1 instanceof String && o2 instanceof String) {
				return collator.compare(o1, o2);
			}
			if (o1 instanceof Comparable && o2 instanceof Comparable) {
				return ((Comparable<T>) o1).compareTo(o2);
			}

			return collator.compare(o1, o2);
		}
	}

	private interface Lock {}
}