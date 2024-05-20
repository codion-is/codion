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
import is.codion.common.event.EventObserver;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

class DefaultFilterComboBoxModel<T> implements FilterComboBoxModel<T> {

	private static final Predicate<?> DEFAULT_ITEM_VALIDATOR = new DefaultValidator<>();
	private static final Function<Object, ?> DEFAULT_SELECTED_ITEM_TRANSLATOR = new DefaultSelectedItemTranslator<>();
	private static final Predicate<?> DEFAULT_VALID_SELECTION_PREDICATE = new DefaultValidSelectionPredicate<>();
	private static final Comparator<?> DEFAULT_COMPARATOR = new DefaultComparator<>();

	private final Event<T> selectionEvent = Event.event();
	private final State selectionEmpty = State.state(true);
	private final State includeNull = State.state();
	private final Value<T> nullItem = Value.value();
	private final State filterSelectedItem = State.state(true);
	private final List<T> visibleItems = new ArrayList<>();
	private final List<T> filteredItems = new ArrayList<>();
	private final Refresher<T> refresher;
	private final Value<Predicate<T>> includeCondition = Value.value();
	private final Value<Predicate<T>> validator = Value.nonNull((Predicate<T>) DEFAULT_ITEM_VALIDATOR).build();
	private final Value<Function<Object, T>> selectedItemTranslator = Value.nonNull((Function<Object, T>) DEFAULT_SELECTED_ITEM_TRANSLATOR).build();
	private final Value<Predicate<T>> validSelectionPredicate = Value.nonNull((Predicate<T>) DEFAULT_VALID_SELECTION_PREDICATE).build();
	private final Value<Comparator<T>> comparator = Value.nullable((Comparator<T>) DEFAULT_COMPARATOR).build();

	/**
	 * set during setItems()
	 */
	private boolean cleared = true;
	private T selectedItem = null;

	/**
	 * Due to a java.util.ConcurrentModificationException in OSX
	 */
	private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

	/**
	 * Instantiates a new FilterComboBoxModel.
	 * The model items are sorted automatically with a default collation based comparator.
	 * To prevent sorting set the comparator to null via {@link #comparator()} before adding items.
	 */
	protected DefaultFilterComboBoxModel() {
		this.refresher = new DefaultRefresher(new DefaultItems());
		includeCondition.addListener(this::filterItems);
		validator.addValidator(validator -> items().stream()
						.filter(Objects::nonNull)
						.forEach(validator::test));
		comparator.addListener(this::sortItems);
		validSelectionPredicate.addValidator(predicate -> {
			if (predicate != null && !predicate.test(selectedItem)) {
				throw new IllegalArgumentException("The current selected item does not satisfy the valid selection predicate");
			}
		});
		includeNull.addConsumer(value -> {
			if (value && !visibleItems.contains(null)) {
				visibleItems.add(0, null);
			}
			else {
				visibleItems.remove(null);
			}
		});
		nullItem.addValidator(this::validate);
	}

	@Override
	public final Refresher<T> refresher() {
		return refresher;
	}

	@Override
	public final void refresh() {
		refreshThen(null);
	}

	@Override
	public final void refreshThen(Consumer<Collection<T>> afterRefresh) {
		refresher.refreshThen(afterRefresh);
	}

	@Override
	public final void clear() {
		setSelectedItem(null);
		setItems(emptyList());
	}

	@Override
	public final boolean cleared() {
		return cleared;
	}

	@Override
	public final void setItems(Collection<T> items) {
		requireNonNull(items);
		filteredItems.clear();
		visibleItems.clear();
		if (includeNull.get()) {
			visibleItems.add(0, null);
		}
		visibleItems.addAll(items.stream()
						.map(this::validate)
						.collect(toList()));
		filterItems();
		cleared = items.isEmpty();
	}

	@Override
	public final void filterItems() {
		visibleItems.addAll(filteredItems);
		filteredItems.clear();
		if (includeCondition.isNotNull()) {
			for (Iterator<T> iterator = visibleItems.listIterator(); iterator.hasNext(); ) {
				T item = iterator.next();
				if (item != null && !includeCondition.get().test(item)) {
					filteredItems.add(item);
					iterator.remove();
				}
			}
		}
		sortItems();
		if (selectedItem != null && visibleItems.contains(selectedItem)) {
			//update the selected item since the underlying data could have changed
			selectedItem = visibleItems.get(visibleItems.indexOf(selectedItem));
		}
		if (selectedItem != null && !visibleItems.contains(selectedItem) && filterSelectedItem.get()) {
			setSelectedItem(null);
		}
		else {
			fireContentsChanged();
		}
	}

	@Override
	public final List<T> visibleItems() {
		if (visibleItems.isEmpty()) {
			return emptyList();
		}
		if (!includeNull.get()) {
			return unmodifiableList(visibleItems);
		}

		return unmodifiableList(visibleItems.subList(1, getSize()));
	}

	@Override
	public final Collection<T> filteredItems() {
		return unmodifiableList(filteredItems);
	}

	@Override
	public final Collection<T> items() {
		List<T> entities = new ArrayList<>(visibleItems());
		entities.addAll(filteredItems);

		return unmodifiableList(entities);
	}

	@Override
	public final Value<Predicate<T>> includeCondition() {
		return includeCondition;
	}

	@Override
	public final int filteredCount() {
		return filteredItems.size();
	}

	@Override
	public final int visibleCount() {
		return visibleItems.size();
	}

	@Override
	public final boolean visible(T item) {
		if (item == null) {
			return includeNull.get();
		}

		return visibleItems.contains(item);
	}

	@Override
	public final boolean filtered(T item) {
		return filteredItems.contains(item);
	}

	@Override
	public final void add(T item) {
		validate(item);
		if (includeCondition.isNull() || includeCondition.get().test(item)) {
			if (!visibleItems.contains(item)) {
				visibleItems.add(item);
				sortItems();
			}
		}
		else if (!filteredItems.contains(item)) {
			filteredItems.add(item);
		}
	}

	@Override
	public final void remove(T item) {
		requireNonNull(item);
		filteredItems.remove(item);
		if (visibleItems.remove(item)) {
			fireContentsChanged();
		}
	}

	@Override
	public final void replace(T item, T replacement) {
		validate(replacement);
		remove(item);
		add(replacement);
		if (Objects.equals(selectedItem, item)) {
			selectedItem = selectedItemTranslator.get().apply(null);
			setSelectedItem(replacement);
		}
	}

	@Override
	public final void sortItems() {
		if (comparator.isNotNull() && !visibleItems.isEmpty()) {
			if (includeNull.get()) {
				visibleItems.remove(0);
			}
			visibleItems.sort(comparator.get());
			if (includeNull.get()) {
				visibleItems.add(0, null);
			}
			fireContentsChanged();
		}
	}

	@Override
	public final boolean containsItem(T item) {
		return visibleItems.contains(item) || filteredItems.contains(item);
	}

	@Override
	public final Value<Comparator<T>> comparator() {
		return comparator;
	}

	@Override
	public final Value<Predicate<T>> validator() {
		return validator;
	}

	@Override
	public final Value<Function<Object, T>> selectedItemTranslator() {
		return selectedItemTranslator;
	}

	@Override
	public final Value<Predicate<T>> validSelectionPredicate() {
		return validSelectionPredicate;
	}

	@Override
	public final State includeNull() {
		return includeNull;
	}

	@Override
	public final Value<T> nullItem() {
		return nullItem;
	}

	@Override
	public final boolean nullSelected() {
		return includeNull.get() && selectedItem == null;
	}

	@Override
	public final StateObserver selectionEmpty() {
		return selectionEmpty.observer();
	}

	@Override
	public final T selectedValue() {
		if (nullSelected()) {
			return null;
		}

		return selectedItem;
	}

	@Override
	public final T getSelectedItem() {
		if (selectedItem == null && nullItem.isNotNull()) {
			return nullItem.get();
		}

		return selectedItem;
	}

	@Override
	public final void setSelectedItem(Object item) {
		T toSelect = selectedItemTranslator.get().apply(Objects.equals(nullItem.get(), item) ? null : item);
		if (!Objects.equals(selectedItem, toSelect) && validSelectionPredicate.get().test(toSelect)) {
			selectedItem = toSelect;
			fireContentsChanged();
			selectionEmpty.set(selectedValue() == null);
			selectionEvent.accept(selectedItem);
		}
	}

	@Override
	public final State filterSelectedItem() {
		return filterSelectedItem;
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
		T element = visibleItems.get(index);
		if (element == null) {
			return nullItem.get();
		}

		return element;
	}

	@Override
	public final int getSize() {
		return visibleItems.size();
	}

	@Override
	public final <V> Value<V> createSelectorValue(ItemFinder<T, V> itemFinder) {
		return new SelectorValue<>(itemFinder);
	}

	@Override
	public final EventObserver<T> selectionEvent() {
		return selectionEvent.observer();
	}

	private void fireContentsChanged() {
		ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
		for (ListDataListener dataListener : listDataListeners) {
			dataListener.contentsChanged(event);
		}
	}

	private T validate(T item) {
		if (!validator.get().test(item)) {
			throw new IllegalArgumentException("Invalid item: " + item);
		}

		return item;
	}

	private final class SelectorValue<V> extends AbstractValue<V> {

		private final ItemFinder<T, V> itemFinder;

		private SelectorValue(ItemFinder<T, V> itemFinder) {
			this.itemFinder = requireNonNull(itemFinder);
			selectionEvent.addListener(this::notifyListeners);
		}

		@Override
		public V get() {
			if (selectionEmpty.get()) {
				return null;
			}

			return itemFinder.value(selectedValue());
		}

		@Override
		protected void setValue(V value) {
			setSelectedItem(value == null ? null : itemFinder.findItem(visibleItems(), value));
		}
	}

	private final class DefaultRefresher extends AbstractFilterModelRefresher<T> {

		private DefaultRefresher(Supplier<Collection<T>> items) {
			super(items);
		}

		@Override
		protected void processResult(Collection<T> items) {
			setItems(items);
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

	private final class DefaultItems implements Supplier<Collection<T>> {

		@Override
		public Collection<T> get() {
			return items();
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