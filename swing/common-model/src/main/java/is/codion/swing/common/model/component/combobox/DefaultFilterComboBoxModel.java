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

import is.codion.common.model.selection.SingleSelection;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Text;
import is.codion.common.utilities.item.Item;
import is.codion.swing.common.model.component.list.AbstractRefreshWorker;

import org.jspecify.annotations.Nullable;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static is.codion.common.reactive.value.Value.Notify.SET;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class DefaultFilterComboBoxModel<T> implements FilterComboBoxModel<T> {

	private static final Function<Object, ?> DEFAULT_SELECTED_ITEM_TRANSLATOR = new DefaultSelectedItemTranslator<>();
	private static final Comparator<?> DEFAULT_COMPARATOR = new DefaultComparator<>();
	private static final Comparator<?> NULL_COMPARATOR = new NullComparator<>();

	private final DefaultComboBoxSelection selection;
	private final Sort<T> sort;
	private final DefaultComboBoxItems modelItems;

	/**
	 * Due to a java.util.ConcurrentModificationException in OSX
	 */
	private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

	private DefaultFilterComboBoxModel(DefaultBuilder<T> builder) {
		selection = new DefaultComboBoxSelection(builder.translator);
		builder.onSelection.forEach(onSelection ->
						selection.item().addConsumer(onSelection));
		sort = new DefaultComboBoxSort<>(builder.comparator);
		modelItems = new DefaultComboBoxItems(builder);
		selection.item().set(builder.selectItem);
	}

	@Override
	public ComboBoxItems<T> items() {
		return modelItems;
	}

	@Override
	public Sort<T> sort() {
		return sort;
	}

	@Override
	public SingleSelection<T> selection() {
		return selection;
	}

	@Override
	public @Nullable T getSelectedItem() {
		if (selection.selected.item == null) {
			return modelItems.nullItem;
		}

		return selection.selected.item;
	}

	@Override
	public void setSelectedItem(@Nullable Object item) {
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
	public @Nullable T getElementAt(int index) {
		T element = modelItems.included.items.get(index);
		if (element == null) {
			return modelItems.nullItem;
		}

		return element;
	}

	@Override
	public int getSize() {
		return modelItems.included.items.size();
	}

	@Override
	public <V> Value<V> createSelector(ItemFinder<T, V> itemFinder) {
		return new SelectorValue<>(itemFinder);
	}

	private void fireContentsChanged() {
		ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
		for (ListDataListener dataListener : listDataListeners) {
			dataListener.contentsChanged(event);
		}
	}

	private static final class DefaultItemsStep implements Builder.ItemsStep {

		@Override
		public <T> Builder<T> items(Collection<T> items) {
			return new DefaultFilterComboBoxModel.DefaultBuilder<>(requireNonNull(items), null);
		}

		@Override
		public <T> Builder<T> items(Supplier<Collection<T>> items) {
			return new DefaultFilterComboBoxModel.DefaultBuilder<>(null, requireNonNull(items));
		}

		@Override
		public <T> ItemComboBoxModelBuilder<T> items(List<Item<T>> items) {
			return new DefaultFilterComboBoxModel.DefaultItemComboBoxModelBuilder<>(items);
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		static final DefaultItemsStep ITEMS = new DefaultItemsStep();

		private final @Nullable Collection<T> items;
		private final @Nullable Supplier<Collection<T>> supplier;
		private final Collection<Consumer<T>> onSelection = new ArrayList<>(1);

		private Comparator<T> comparator = (Comparator<T>) DEFAULT_COMPARATOR;
		private Function<Object, T> translator = (Function<Object, T>) DEFAULT_SELECTED_ITEM_TRANSLATOR;
		private boolean async = ASYNC.getOrThrow();
		private @Nullable Consumer<Exception> onRefreshException;
		private boolean filterSelected;
		private boolean includeNull;
		private @Nullable T nullItem;
		private @Nullable T selectItem;
		private boolean refresh = false;

		private DefaultBuilder(@Nullable Collection<T> items, @Nullable Supplier<Collection<T>> supplier) {
			this.items = items;
			this.supplier = supplier;
		}

		@Override
		public Builder<T> comparator(@Nullable Comparator<T> comparator) {
			this.comparator = comparator == null ? (Comparator<T>) NULL_COMPARATOR : comparator;
			return this;
		}

		@Override
		public Builder<T> includeNull(boolean includeNull) {
			this.includeNull = includeNull;
			return this;
		}

		@Override
		public Builder<T> nullItem(@Nullable T nullItem) {
			this.nullItem = nullItem;

			return includeNull(nullItem != null);
		}

		@Override
		public Builder<T> select(@Nullable T item) {
			this.selectItem = item;
			return this;
		}

		@Override
		public Builder<T> translator(Function<Object, T> translator) {
			this.translator = requireNonNull(translator);
			return this;
		}

		@Override
		public Builder<T> filterSelected(boolean filterSelected) {
			this.filterSelected = filterSelected;
			return this;
		}

		@Override
		public Builder<T> onSelection(Consumer<@Nullable T> onSelection) {
			this.onSelection.add(requireNonNull(onSelection));
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
		public Builder<T> refresh(boolean refresh) {
			this.refresh = refresh;
			return this;
		}

		@Override
		public FilterComboBoxModel<T> build() {
			return new DefaultFilterComboBoxModel<>(this);
		}
	}

	static final class DefaultItemComboBoxModelBuilder<T> implements ItemComboBoxModelBuilder<T> {

		private final List<Item<T>> items;
		private final @Nullable Item<T> nullItem;

		private boolean sorted = false;
		private @Nullable Comparator<Item<T>> comparator;
		private @Nullable Item<T> selected;

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
		public ItemComboBoxModelBuilder<T> selected(@Nullable T selected) {
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

	private static final class DefaultComboBoxSort<T> implements Sort<T> {

		private final Comparator<T> comparator;
		private final Event<Boolean> event = Event.event();

		private DefaultComboBoxSort(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(T o1, T o2) {
			return comparator.compare(o1, o2);
		}

		@Override
		public boolean sorted() {
			return comparator != NULL_COMPARATOR;
		}

		@Override
		public Observer<Boolean> observer() {
			return event.observer();
		}
	}

	private final class DefaultComboBoxItems implements ComboBoxItems<T> {

		private final Lock lock = new Lock() {};

		private final AbstractRefresher<T> refresher;
		private final DefaultIncludedItems included;
		private final DefaultFilteredItems filtered = new DefaultFilteredItems();

		private final boolean filterSelected;
		private final boolean includeNull;
		private final @Nullable T nullItem;

		private boolean cleared = true;

		private DefaultComboBoxItems(DefaultBuilder<T> builder) {
			this.includeNull = builder.includeNull;
			this.nullItem = builder.nullItem;
			this.filterSelected = builder.filterSelected;
			this.included = new DefaultIncludedItems();
			if (includeNull) {
				included.items.add(null);
			}
			included.predicate.addListener(this::filter);
			if (builder.items != null) {
				set(builder.items);
			}
			refresher = new DefaultRefreshWorker(builder.supplier, builder.async, builder.onRefreshException);
			if (builder.items == null && builder.refresh) {
				refresher.refresh(null);
			}
		}

		@Override
		public Refresher<T> refresher() {
			return refresher;
		}

		@Override
		public void refresh() {
			refresher.refresh(null);
		}

		@Override
		public void refresh(Consumer<Collection<T>> onResult) {
			refresher.refresh(requireNonNull(onResult));
		}

		@Override
		public Collection<T> get() {
			synchronized (lock) {
				List<T> includedItems = included.get();
				if (filtered.items.isEmpty()) {
					return unmodifiableCollection(new ArrayList<>(includedItems));
				}
				List<T> entities = new ArrayList<>(includedItems.size() + filtered.items.size());
				entities.addAll(includedItems);
				entities.addAll(filtered.items);

				return unmodifiableList(entities);
			}
		}

		@Override
		public void set(Collection<T> items) {
			requireNonNull(items);
			synchronized (lock) {
				filtered.items.clear();
				included.items.clear();
				if (includeNull) {
					included.items.add(0, null);
				}
				//remove duplicates while preserving the original order
				included.items.addAll(new LinkedHashSet<>(items));
				filterInternal();
				replaceSelectedItem();
				cleared = items.isEmpty();
				included.sortInternal();
				included.notifyChanges();
				filtered.notifyChanges();
			}
		}

		@Override
		public void add(T item) {
			requireNonNull(item);
			synchronized (lock) {
				if (included(item)) {
					if (!included.items.contains(item)) {
						included.items.add(item);
						included.sortInternal();
						included.added.accept(singleton(item));
						included.notifyChanges();
					}
				}
				else {
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
				else if (included.items.remove(item)) {
					included.notifyChanges();
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
		public void remove(Predicate<T> predicate) {
			requireNonNull(predicate);
			synchronized (lock) {
				remove(Stream.concat(included.items.stream(), filtered.items.stream())
								.filter(predicate)
								.collect(toList()));
			}
		}

		@Override
		public IncludedItems<T> included() {
			return included;
		}

		@Override
		public FilteredItems<T> filtered() {
			return filtered;
		}

		@Override
		public boolean contains(T item) {
			synchronized (lock) {
				return included.items.contains(item) || filtered.items.contains(item);
			}
		}

		@Override
		public int size() {
			synchronized (lock) {
				return included.size() + filtered.size();
			}
		}

		@Override
		public void filter() {
			synchronized (lock) {
				if (size() > 0) {
					included.items.addAll(filtered.items);
					filtered.items.clear();
					filterInternal();
					included.sortInternal();
					if (filterSelected
									&& selection.selected.item != null
									&& !included.items.contains(selection.selected.item)) {
						selection.selected.setSelectedItem(null);
					}
					included.notifyChanges();
					filtered.notifyChanges();
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
			replace(singletonMap(requireNonNull(item), requireNonNull(replacement)));
			if (Objects.equals(selection.selected.item, item)) {
				selection.selected.replaceWith(replacement);
			}
		}

		@Override
		public void replace(Map<T, T> items) {
			// Note: Similar logic exists in DefaultFilterModelItems in common-model module.
			// Both implementations handle item replacement with filtering but have different collection types
			// and threading requirements, making extraction to a common utility non-trivial.
			requireNonNull(items);
			synchronized (lock) {
				Map<T, T> replacements = new HashMap<>(items);
				for (T itemToReplace : items.keySet()) {
					if (filtered.items.remove(itemToReplace)) {
						T replacement = replacements.remove(itemToReplace);
						if (included(replacement)) {
							included.items.add(replacement);
						}
						else {
							filtered.items.add(replacement);
						}
					}
				}
				ListIterator<T> iterator = included.items.listIterator();
				while (!replacements.isEmpty() && iterator.hasNext()) {
					T item = iterator.next();
					T replacement = replacements.remove(item);
					if (replacement != null) {
						if (included(replacement)) {
							iterator.set(replacement);
						}
						else {
							iterator.remove();
							filtered.items.add(replacement);
						}
					}
				}
				included.notifyChanges();
				filtered.notifyChanges();
				included.sort();
			}
		}

		@Override
		public boolean cleared() {
			return cleared;
		}

		private void filterInternal() {
			Predicate<T> predicate = included.predicate.get();
			if (predicate != null) {
				for (Iterator<T> iterator = included.items.listIterator(); iterator.hasNext(); ) {
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
				else if (!modelItems.included.items.isEmpty()) {
					setSelectedItem(modelItems.included.items.get(0));
				}
			}
		}

		private void replaceSelectedItem() {
			if (selection.selected.item != null) {
				int index = included.items.indexOf(selection.selected.item);
				if (index != -1) {
					//update the selected item since the underlying data could have changed
					selection.selected.item = included.items.get(index);
				}
			}
		}

		private boolean included(T item) {
			return included.predicate.isNull() || included.predicate.getOrThrow().test(item);
		}

		private final class DefaultRefreshWorker extends AbstractRefreshWorker<T> {

			private DefaultRefreshWorker(@Nullable Supplier<Collection<T>> supplier, boolean async,
																	 @Nullable Consumer<Exception> onException) {
				super(supplier, async, onException);
			}

			@Override
			protected void processResult(Collection<T> result) {
				set(result);
			}
		}

		private final class DefaultIncludedItems implements IncludedItems<T> {

			private final IncludePredicate<T> predicate = new DefaultIncludePredicate<>();
			private final List<@Nullable T> items = new ArrayList<>();
			private final Event<List<T>> changed = Event.event();
			private final Event<Collection<T>> added = Event.event();

			@Override
			public IncludePredicate<T> predicate() {
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
				return changed.observer();
			}

			@Override
			public Observer<Collection<T>> added() {
				return added.observer();
			}

			@Override
			public SingleSelection<T> selection() {
				return selection;
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
			public @Nullable T get(int index) {
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
			public int size() {
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
			public void sort() {
				synchronized (lock) {
					if (sortInternal()) {
						notifyChanges();
					}
				}
			}

			private boolean sortInternal() {
				if (sort.sorted() && size() > 0) {
					items.subList(includeNull ? 1 : 0, items.size()).sort(sort);
					return true;
				}

				return false;
			}

			private void notifyChanges() {
				fireContentsChanged();
				changed.accept(get());
			}
		}

		private final class DefaultFilteredItems implements FilteredItems<T> {

			private final Event<Collection<T>> changed = Event.event();
			private final Set<T> items = new LinkedHashSet<>();

			@Override
			public Collection<T> get() {
				synchronized (lock) {
					return unmodifiableCollection(items);
				}
			}

			@Override
			public boolean contains(T item) {
				synchronized (lock) {
					return items.contains(item);
				}
			}

			@Override
			public int size() {
				synchronized (lock) {
					return items.size();
				}
			}

			@Override
			public Observer<Collection<T>> observer() {
				return changed.observer();
			}

			private void notifyChanges() {
				changed.accept(get());
			}
		}
	}

	private final class DefaultComboBoxSelection implements SingleSelection<T> {

		private final SelectedItem selected;

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
		public Value<T> item() {
			return selected;
		}

		@Override
		public void clear() {
			selected.clear();
		}
	}

	private final class SelectedItem extends AbstractValue<T> {

		private final Event<T> changing = Event.event();
		private final State empty = State.state(true);
		private final Function<@Nullable Object, T> translator;

		private @Nullable T item = null;

		private SelectedItem(Function<Object, T> translator) {
			this.translator = translator;
			addListener(DefaultFilterComboBoxModel.this::fireContentsChanged);
		}

		@Override
		protected @Nullable T getValue() {
			return item;
		}

		@Override
		protected void setValue(@Nullable T value) {
			setSelectedItem(value);
		}

		private void setSelectedItem(@Nullable Object item) {
			T toSelect = translator.apply(Objects.equals(modelItems.nullItem, item) ? null : item);
			if (!Objects.equals(this.item, toSelect)) {
				changing.accept(toSelect);
				this.item = toSelect;
				empty.set(toSelect == null);
				notifyObserver();
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
			selection.selected.addListener(this::notifyObserver);
		}

		@Override
		protected @Nullable V getValue() {
			if (selection.selected.empty.is()) {
				return null;
			}

			return itemFinder.value(selection.item().getOrThrow());
		}

		@Override
		protected void setValue(@Nullable V value) {
			setSelectedItem(value == null ? null : itemFinder.findItem(modelItems.included.get(), value).orElse(null));
		}
	}

	private static final class DefaultIncludePredicate<R>
					extends AbstractValue<Predicate<R>> implements IncludePredicate<R> {

		private @Nullable Predicate<R> predicate;

		private DefaultIncludePredicate() {
			super(SET);
		}

		@Override
		protected @Nullable Predicate<R> getValue() {
			return predicate;
		}

		@Override
		protected void setValue(@Nullable Predicate<R> predicate) {
			this.predicate = predicate;
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
							.collect(toMap(Item::get, Function.identity()));
		}

		@Override
		public Item<T> apply(Object item) {
			if (item instanceof Item) {
				return itemMap.get(((Item<T>) item).get());
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