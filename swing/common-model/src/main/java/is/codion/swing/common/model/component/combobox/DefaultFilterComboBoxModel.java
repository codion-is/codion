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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.observer.Mutable;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.AbstractFilterModelRefresher;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.value.Value.Notify.WHEN_SET;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultFilterComboBoxModel<T> implements FilterComboBoxModel<T> {

	private static final Function<Object, ?> DEFAULT_SELECTED_ITEM_TRANSLATOR = new DefaultSelectedItemTranslator<>();
	private static final Comparator<?> DEFAULT_COMPARATOR = new DefaultComparator<>();

	private final DefaultComboBoxSelection selection;
	private final DefaultComboBoxItems modelItems;
	private final DefaultRefresher refresher;

	/**
	 * Due to a java.util.ConcurrentModificationException in OSX
	 */
	private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

	private DefaultFilterComboBoxModel(DefaultBuilder<T> builder) {
		selection = new DefaultComboBoxSelection(builder.translator);
		modelItems = new DefaultComboBoxItems(builder.includeNull, builder.nullItem, builder.comparator);
		if (builder.supplier == null) {
			refresher = new DefaultRefresher(new DefaultItemsSupplier());
			if (builder.items != null) {
				modelItems.set(builder.items);
			}
		}
		else {
			refresher = new DefaultRefresher(builder.supplier);
		}
	}

	@Override
	public Refresher<T> refresher() {
		return refresher;
	}

	@Override
	public void refresh() {
		refresh(null);
	}

	@Override
	public void refresh(Consumer<Collection<T>> onRefresh) {
		refresher.doRefresh(onRefresh);
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
		return selection.selected.get();
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
			this.comparator = comparator;
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

	private final class DefaultComboBoxItems implements ComboBoxItems<T> {

		private final DefaultVisibleItems visible;
		private final DefaultFilteredItems filtered = new DefaultFilteredItems();
		private final Event<Collection<T>> event = Event.event();

		private final boolean includeNull;
		private final T nullItem;

		private boolean cleared = true;

		private DefaultComboBoxItems(boolean includeNull, T nullItem, Comparator<T> comparator) {
			this.includeNull = includeNull;
			this.nullItem = nullItem;
			this.visible = new DefaultVisibleItems(comparator);
			if (includeNull) {
				visible.items.add(null);
			}
			visible.predicate.addListener(this::filter);
		}

		@Override
		public Collection<T> get() {
			List<T> visibleItems = visible.get();
			if (filtered.items.isEmpty()) {
				return unmodifiableCollection(new ArrayList<>(visibleItems));
			}
			List<T> entities = new ArrayList<>(visibleItems.size() + filtered.items.size());
			entities.addAll(visibleItems);
			entities.addAll(filtered.items);

			return unmodifiableList(entities);
		}

		@Override
		public void set(Collection<T> items) {
			requireNonNull(items);
			filtered.items.clear();
			visible.items.clear();
			if (includeNull) {
				visible.items.add(0, null);
			}
			visible.items.addAll(items);
			// Notifies both visible and filtered
			filter(false);
			cleared = items.isEmpty();
			event.accept(items);
		}

		@Override
		public boolean addItem(T item) {
			requireNonNull(item);
			if (visible.predicate.isNull() || visible.predicate.get().test(item)) {
				if (!visible.items.contains(item)) {
					visible.items.add(item);
					visible.sort();

					return true;
				}
			}
			else if (!filtered.items.contains(item)) {
				filtered.items.add(item);
				filtered.notifyChanges();
			}

			return false;
		}

		@Override
		public boolean addItems(Collection<T> items) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeItem(T item) {
			requireNonNull(item);
			if (filtered.items.remove(item)) {
				filtered.notifyChanges();
			}
			if (visible.items.remove(item)) {
				fireContentsChanged();
				visible.notifyChanges();

				return true;
			}

			return false;
		}

		@Override
		public boolean removeItems(Collection<T> items) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Observer<Collection<T>> observer() {
			return event.observer();
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
			return visible.items.contains(item) || filtered.items.contains(item);
		}

		@Override
		public int count() {
			return visible.count() + filtered.count();
		}

		@Override
		public void filter() {
			filter(selection.filterSelected.get());
		}

		@Override
		public void clear() {
			setSelectedItem(null);
			set(emptyList());
		}

		@Override
		public void replace(T item, T replacement) {
			requireNonNull(item);
			requireNonNull(replacement);
			removeItem(item);
			addItem(replacement);
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
			if (visible.predicate.isNotNull()) {
				for (Iterator<T> iterator = visible.items.listIterator(); iterator.hasNext(); ) {
					T item = iterator.next();
					if (item != null && !visible.predicate.get().test(item)) {
						filtered.items.add(item);
						iterator.remove();
					}
				}
			}
			visible.sort();
			if (selection.selected.item != null) {
				int index = visible.items.indexOf(selection.selected.item);
				if (index != -1) {
					//update the selected item since the underlying data could have changed
					selection.selected.item = visible.items.get(index);
				}
			}
			if (filterSelectedItem
							&& selection.selected.item != null
							&& !visible.items.contains(selection.selected.item)) {
				selection.selected.setSelectedItem(null);
			}
			else {
				fireContentsChanged();
			}
			visible.notifyChanges();
			filtered.notifyChanges();
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
			this.comparator = comparator;
		}

		@Override
		public Value<Predicate<T>> predicate() {
			return predicate;
		}

		@Override
		public List<T> get() {
			if (items.isEmpty()) {
				return emptyList();
			}
			if (!modelItems.includeNull) {
				return unmodifiableList(items);
			}

			return unmodifiableList(items.subList(1, getSize()));
		}

		@Override
		public Observer<List<T>> observer() {
			return event.observer();
		}

		@Override
		public boolean contains(T item) {
			if (item == null) {
				return modelItems.includeNull;
			}

			return items.contains(item);
		}

		@Override
		public int indexOf(T item) {
			return items.indexOf(item);
		}

		@Override
		public T itemAt(int index) {
			return items.get(index);
		}

		@Override
		public boolean addItemAt(int index, T item) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addItemsAt(int index, Collection<T> items) {
			throw new UnsupportedOperationException();
		}

		@Override
		public T removeItemAt(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<T> removeItems(int fromIndex, int toIndex) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setItemAt(int index, T item) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int count() {
			if (items.isEmpty()) {
				return 0;
			}
			if (!modelItems.includeNull) {
				return items.size();
			}

			return items.size() - 1;
		}

		@Override
		public void sort() {
			if (comparator != null && !items.isEmpty()) {
				if (modelItems.includeNull) {
					items.remove(0);
				}
				items.sort(comparator);
				if (modelItems.includeNull) {
					items.add(0, null);
				}
				fireContentsChanged();
				notifyChanges();
			}
		}

		private void notifyChanges() {
			event.accept(get());
		}
	}

	private final class DefaultFilteredItems implements FilteredItems<T> {

		private final List<T> items = new ArrayList<>();
		private final Event<Collection<T>> event = Event.event();

		@Override
		public Collection<T> get() {
			return unmodifiableCollection(items);
		}

		@Override
		public Observer<Collection<T>> observer() {
			return event.observer();
		}

		@Override
		public boolean contains(T item) {
			return items.contains(item);
		}

		@Override
		public int count() {
			return items.size();
		}

		private void notifyChanges() {
			event.accept(get());
		}
	}

	private final class DefaultComboBoxSelection implements ComboBoxSelection<T> {

		private final Selected selected;
		private final State filterSelected = State.state(false);

		private DefaultComboBoxSelection(Function<Object, T> translator) {
			selected = new Selected(translator);
		}

		@Override
		public StateObserver empty() {
			return selected.empty.observer();
		}

		@Override
		public Observer<?> changing() {
			return selected.changing;
		}

		@Override
		public Mutable<T> item() {
			return selected;
		}

		@Override
		public T value() {
			if (nullSelected()) {
				return null;
			}

			return selected.item;
		}

		@Override
		public boolean nullSelected() {
			return modelItems.includeNull && selected.item == null;
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

	private final class Selected implements Mutable<T> {

		private final Event<T> changing = Event.event();
		private final Event<T> event = Event.event();
		private final State empty = State.state(true);
		private final Function<Object, T> translator;

		private T item = null;

		private Selected(Function<Object, T> translator) {
			this.translator = translator;
		}

		@Override
		public T get() {
			if (item == null) {
				return modelItems.nullItem;
			}

			return item;
		}

		@Override
		public void set(T item) {
			setSelectedItem(item);
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
				empty.set(selection.value() == null);
				fireContentsChanged();
				event.accept(this.item);
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

			return itemFinder.value(selection.value());
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
		protected void processResult(Collection<T> items) {
			modelItems.set(items);
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

	private final class DefaultItemsSupplier implements Supplier<Collection<T>> {

		@Override
		public Collection<T> get() {
			return modelItems.get();
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
}