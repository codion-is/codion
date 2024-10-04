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

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

class DefaultFilterComboBoxModel<T> implements FilterComboBoxModel<T> {

	private static final Predicate<?> DEFAULT_ITEM_VALIDATOR = new DefaultValidator<>();
	private static final Function<Object, ?> DEFAULT_SELECTED_ITEM_TRANSLATOR = new DefaultSelectedItemTranslator<>();
	private static final Predicate<?> DEFAULT_VALID_SELECTION_PREDICATE = new DefaultValidSelectionPredicate<>();
	private static final Comparator<?> DEFAULT_COMPARATOR = new DefaultComparator<>();

	private final DefaultComboBoxSelection selectionModel = new DefaultComboBoxSelection();
	private final DefaultComboBoxItems modelItems = new DefaultComboBoxItems();
	private final DefaultRefresher refresher;

	/**
	 * Due to a java.util.ConcurrentModificationException in OSX
	 */
	private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

	/**
	 * Instantiates a new FilterComboBoxModel.
	 * The model items are sorted automatically with a default collation based comparator.
	 * To prevent sorting set the comparator to null via {@link VisibleItems#comparator()} before adding items.
	 */
	protected DefaultFilterComboBoxModel() {
		refresher = new DefaultRefresher(new DefaultItemsSupplier());
	}

	@Override
	public final Refresher<T> refresher() {
		return refresher;
	}

	@Override
	public final void refresh() {
		refresh(null);
	}

	@Override
	public final void refresh(Consumer<Collection<T>> onRefresh) {
		refresher.doRefresh(onRefresh);
	}

	@Override
	public final ComboBoxItems<T> items() {
		return modelItems;
	}

	@Override
	public final ComboBoxSelection<T> selection() {
		return selectionModel;
	}

	@Override
	public final T getSelectedItem() {
		return selectionModel.selected.get();
	}

	@Override
	public final void setSelectedItem(Object item) {
		selectionModel.selected.setSelectedItem(item);
	}

	@Override
	public final void addListDataListener(ListDataListener listener) {
		requireNonNull(listener, "listener");
		listDataListeners.add(listener);
	}

	@Override
	public final void removeListDataListener(ListDataListener listener) {
		requireNonNull(listener, "listener");
		listDataListeners.remove(listener);
	}

	@Override
	public final T getElementAt(int index) {
		T element = modelItems.visible.items.get(index);
		if (element == null) {
			return modelItems.nullItem.get();
		}

		return element;
	}

	@Override
	public final int getSize() {
		return modelItems.visible.items.size();
	}

	@Override
	public final <V> Value<V> createSelectorValue(ItemFinder<T, V> itemFinder) {
		return new SelectorValue<>(itemFinder);
	}

	private void fireContentsChanged() {
		ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
		for (ListDataListener dataListener : listDataListeners) {
			dataListener.contentsChanged(event);
		}
	}

	private final class DefaultNullItem implements NullItem<T> {

		private final State include = State.builder()
						.consumer(this::includeNullItem)
						.build();
		private final Value<T> item = Value.builder()
						.<T>nullable()
						.build();

		@Override
		public State include() {
			return include;
		}

		@Override
		public void set(T value) {
			item.set(value);
		}

		@Override
		public T get() {
			return item.get();
		}

		@Override
		public Observer<T> observer() {
			return item.observer();
		}

		private void includeNullItem(boolean includeNull) {
			if (includeNull && !modelItems.visible.items.contains(null)) {
				modelItems.visible.items.add(0, null);
			}
			else {
				modelItems.visible.items.remove(null);
			}
		}
	}

	private final class DefaultComboBoxItems implements ComboBoxItems<T> {

		private final Value<Predicate<T>> validator = Value.builder()
						.nonNull((Predicate<T>) DEFAULT_ITEM_VALIDATOR)
						.build();
		private final Value<Predicate<T>> visiblePredicate = Value.builder()
						.<Predicate<T>>nullable()
						.listener(this::filter)
						.build();
		private final DefaultVisibleItems visible = new DefaultVisibleItems();
		private final DefaultFilteredItems filtered = new DefaultFilteredItems();
		private final DefaultNullItem nullItem = new DefaultNullItem();

		private final Event<Collection<T>> event = Event.event();

		private boolean cleared = true;

		private DefaultComboBoxItems() {
			validator.addValidator(validator -> get().stream()
							.filter(Objects::nonNull)
							.forEach(validator::test));
			nullItem.item.addValidator(this::validate);
			visible.comparator.addListener(visible::sort);
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
			if (nullItem.include.get()) {
				visible.items.add(0, null);
			}
			visible.items.addAll(items.stream()
							.map(this::validate)
							.collect(toList()));
			// Notifies both visible and filtered
			filter();
			cleared = items.isEmpty();
			event.accept(items);
		}

		@Override
		public boolean addItem(T item) {
			requireNonNull(item);
			validate(item);
			if (visiblePredicate.isNull() || visiblePredicate.get().test(item)) {
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
			visible.items.addAll(filtered.items);
			filtered.items.clear();
			if (visiblePredicate.isNotNull()) {
				for (Iterator<T> iterator = visible.items.listIterator(); iterator.hasNext(); ) {
					T item = iterator.next();
					if (item != null && !visiblePredicate.get().test(item)) {
						filtered.items.add(item);
						iterator.remove();
					}
				}
			}
			visible.sort();
			if (selectionModel.selected.item != null && visible.items.contains(selectionModel.selected.item)) {
				//update the selected item since the underlying data could have changed
				selectionModel.selected.item = visible.items.get(visible.items.indexOf(selectionModel.selected.item));
			}
			if (selectionModel.selected.item != null
							&& !visible.items.contains(selectionModel.selected.item)
							&& selectionModel.filterSelectedItem.get()) {
				selectionModel.selected.setSelectedItem(null);
			}
			else {
				fireContentsChanged();
			}
			visible.notifyChanges();
			filtered.notifyChanges();
		}

		@Override
		public void clear() {
			setSelectedItem(null);
			set(emptyList());
		}

		@Override
		public NullItem<T> nullItem() {
			return nullItem;
		}

		@Override
		public Value<Predicate<T>> validator() {
			return validator;
		}

		@Override
		public void replace(T item, T replacement) {
			requireNonNull(item);
			requireNonNull(replacement);
			validate(replacement);
			removeItem(item);
			addItem(replacement);
			if (Objects.equals(selectionModel.selected.item, item)) {
				selectionModel.selected.replaceWith(item);
			}
		}

		@Override
		public boolean cleared() {
			return cleared;
		}

		private T validate(T item) {
			if (!validator.get().test(item)) {
				throw new IllegalArgumentException("Invalid item: " + item);
			}

			return item;
		}
	}

	private final class DefaultVisibleItems implements VisibleItems<T> {

		private final Value<Comparator<T>> comparator = Value.builder()
						.nullable((Comparator<T>) DEFAULT_COMPARATOR)
						.build();
		private final List<T> items = new ArrayList<>();
		private final Event<List<T>> event = Event.event();

		@Override
		public Value<Predicate<T>> predicate() {
			return modelItems.visiblePredicate;
		}

		@Override
		public List<T> get() {
			if (items.isEmpty()) {
				return emptyList();
			}
			if (!modelItems.nullItem.include.get()) {
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
				return modelItems.nullItem.include.get();
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
			if (!modelItems.nullItem.include.get()) {
				return items.size();
			}

			return items.size() - 1;
		}

		@Override
		public Value<Comparator<T>> comparator() {
			return comparator;
		}

		@Override
		public void sort() {
			if (comparator.isNotNull() && !items.isEmpty()) {
				if (modelItems.nullItem.include.get()) {
					items.remove(0);
				}
				items.sort(comparator.get());
				if (modelItems.nullItem.include.get()) {
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

		private final Selected selected = new Selected();
		private final State filterSelectedItem = State.state(false);

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
			return modelItems.nullItem.include.get() && selected.item == null;
		}

		@Override
		public void clear() {
			selected.clear();
		}

		@Override
		public Value<Function<Object, T>> translator() {
			return selected.translator;
		}

		@Override
		public Value<Predicate<T>> validPredicate() {
			return selected.valid;
		}

		@Override
		public State filterSelected() {
			return filterSelectedItem;
		}
	}

	private final class Selected implements Mutable<T> {

		private final Event<T> changing = Event.event();
		private final Event<T> event = Event.event();
		private final State empty = State.state(true);
		private final Value<Function<Object, T>> translator = Value.builder()
						.nonNull((Function<Object, T>) DEFAULT_SELECTED_ITEM_TRANSLATOR)
						.build();
		private final Value<Predicate<T>> valid = Value.builder()
						.nonNull((Predicate<T>) DEFAULT_VALID_SELECTION_PREDICATE)
						.build();

		private T item = null;

		private Selected() {
			valid.addValidator(predicate -> {
				if (predicate != null && !predicate.test(item)) {
					throw new IllegalArgumentException("The current selected item does not satisfy the valid selection predicate");
				}
			});
		}

		@Override
		public T get() {
			if (item == null) {
				return modelItems.nullItem.get();
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
			T toSelect = translator.get().apply(Objects.equals(modelItems.nullItem.get(), item) ? null : item);
			if (!Objects.equals(this.item, toSelect) && valid.get().test(toSelect)) {
				changing.accept(toSelect);
				this.item = toSelect;
				empty.set(selectionModel.value() == null);
				fireContentsChanged();
				event.accept(this.item);
			}
		}

		private void replaceWith(T replacement) {
			selectionModel.selected.item = selectionModel.selected.translator.get().apply(null);
			selectionModel.selected.set(replacement);
		}
	}

	private final class SelectorValue<V> extends AbstractValue<V> {

		private final ItemFinder<T, V> itemFinder;

		private SelectorValue(ItemFinder<T, V> itemFinder) {
			this.itemFinder = requireNonNull(itemFinder);
			selectionModel.selected.event.addListener(this::notifyListeners);
		}

		@Override
		protected V getValue() {
			if (selectionModel.selected.empty.get()) {
				return null;
			}

			return itemFinder.value(selectionModel.value());
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

	private static final class DefaultValidator<T> implements Predicate<T> {

		@Override
		public boolean test(T item) {
			return true;
		}
	}

	private final class DefaultItemsSupplier implements Supplier<Collection<T>> {

		@Override
		public Collection<T> get() {
			return modelItems.get();
		}
	}

	private static final class DefaultValidSelectionPredicate<T> implements Predicate<T> {

		@Override
		public boolean test(T item) {
			return true;
		}
	}

	private static final class DefaultComparator<T> implements Comparator<T> {

		private final Comparator<T> comparator = Text.collator();

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
			if (o1 instanceof Comparable && o2 instanceof Comparable) {
				return ((Comparable<T>) o1).compareTo(o2);
			}
			else {
				return comparator.compare(o1, o2);
			}
		}
	}
}