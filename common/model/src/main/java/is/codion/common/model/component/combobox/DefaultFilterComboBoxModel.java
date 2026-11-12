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
package is.codion.common.model.component.combobox;

import is.codion.common.model.selection.SingleSelection;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.sensor.Sensor;
import is.codion.common.reactive.signal.Signal;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.item.Item;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.reactive.value.Value.Notify.SET;
import static is.codion.common.utilities.Nulls.rejectNulls;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class DefaultFilterComboBoxModel<T> implements FilterComboBoxModel<T> {

	static final Function<Object, ?> DEFAULT_SELECTED_ITEM_TRANSLATOR = new DefaultSelectedItemTranslator<>();
	static final Comparator<?> NULL_COMPARATOR = new NullComparator<>();

	private final DefaultComboBoxSelection selection;
	private final DefaultComboBoxItems modelItems;

	DefaultFilterComboBoxModel(AbstractFilterComboBoxModelBuilder<T, ?> builder) {
		selection = new DefaultComboBoxSelection(builder.translator);
		builder.onSelectedItem.forEach(selection.item()::addConsumer);
		modelItems = new DefaultComboBoxItems(builder, new DefaultComboBoxSort<>(builder.comparator));
		selection.item().set(builder.selectedItem);
	}

	@Override
	public ComboBoxItems<T> items() {
		return modelItems;
	}

	@Override
	public Sort<T> sort() {
		return modelItems.sort();
	}

	@Override
	public SingleSelection<T> selection() {
		return selection;
	}

	@Override
	public @Nullable T selectedItem() {
		if (selection.selected.item == null) {
			return modelItems.nullItem;
		}

		return selection.selected.item;
	}

	@Override
	public <V> Value<V> selector(ItemFinder<T, V> itemFinder) {
		return new SelectorValue<>(itemFinder);
	}

	private static final class DefaultItemsStep implements Builder.ItemsStep {

		@Override
		public <T> Builder<T, ?> items(Collection<T> items) {
			return new DefaultBuilder<>(items);
		}

		@Override
		public <T> Builder<T, ?> items(Supplier<Collection<T>> items) {
			return new DefaultBuilder<>(items);
		}

		@Override
		public <T> ItemComboBoxModelBuilder<T, ?> items(List<Item<T>> items) {
			return new DefaultItemComboBoxModelBuilder<>(items);
		}
	}

	/**
	 * @param items the items
	 * @param <T> the item type
	 * @return a builder for the given items, the {@link Item} based overload notwithstanding
	 */
	static <T> Builder<T, ?> builder(Collection<T> items) {
		return new DefaultBuilder<>(items);
	}

	static final class DefaultBuilder<T> extends AbstractFilterComboBoxModelBuilder<T, DefaultBuilder<T>> {

		static final DefaultItemsStep ITEMS = new DefaultItemsStep();

		private DefaultBuilder(Collection<T> items) {
			super(items);
		}

		private DefaultBuilder(Supplier<Collection<T>> supplier) {
			super(supplier);
		}
	}

	static final class DefaultItemComboBoxModelBuilder<T> extends AbstractItemComboBoxModelBuilder<T, DefaultItemComboBoxModelBuilder<T>> {

		private DefaultItemComboBoxModelBuilder(List<Item<T>> items) {
			super(items);
		}
	}

	private static final class DefaultComboBoxSort<T> implements Sort<T> {

		private final Comparator<T> comparator;
		//the comparator is immutable so the sort never changes, this signal is never fired
		private final Signal<Boolean> signal = Signal.signal();

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
			return signal.observer();
		}
	}

	private final class DefaultComboBoxItems implements ComboBoxItems<T> {

		private final Lock lock = new Lock() {};

		private final Sort<T> sort;
		private final Refresher<T> refresher;
		//every item, included and excluded, in natural order: as set, then added, the order an unsorted model shows
		private final List<T> natural = new ArrayList<>();
		private final DefaultIncludedItems included;
		private final DefaultExcludedItems excluded = new DefaultExcludedItems();
		private final Predicate<T> validator;

		private final boolean filterSelected;
		private final boolean nullable;
		private final @Nullable T nullItem;

		private boolean cleared = true;

		private DefaultComboBoxItems(AbstractFilterComboBoxModelBuilder<T, ?> builder, Sort<T> sort) {
			this.nullable = builder.nullable;
			this.nullItem = builder.nullItem;
			this.filterSelected = builder.filterSelected;
			this.validator = builder.validator;
			this.sort = sort;
			this.included = new DefaultIncludedItems();
			if (nullable) {
				included.items.add(null);
			}
			included.predicate.addListener(this::filter);
			if (builder.items != null) {
				set(builder.items);
			}
			refresher = Refresher.<T>builder()
							.items(builder.supplier)
							.onResult(this::set)
							.onException(builder.onRefreshException)
							.build();
			if (builder.items == null && builder.refresh) {
				refresher.refresh();
			}
		}

		@Override
		public Refresher<T> refresher() {
			return refresher;
		}

		@Override
		public Sort<T> sort() {
			return sort;
		}

		@Override
		public void refresh() {
			refresher.refresh();
		}

		@Override
		public void refresh(Consumer<Collection<T>> onResult) {
			refresher.refresh(requireNonNull(onResult));
		}

		@Override
		public Collection<T> get() {
			synchronized (lock) {
				return unmodifiableList(new ArrayList<>(natural));
			}
		}

		@Override
		public void set(Collection<T> items) {
			//null is the reserved sentinel (see nullable), it must not appear among the actual items
			requireNonNull(items).forEach(this::validate);
			synchronized (lock) {
				natural.clear();
				//remove duplicates while preserving the original order
				natural.addAll(new LinkedHashSet<>(items));
				filterInternal();
				if (filterSelected
								&& selection.selected.item != null
								&& !included.items.contains(selection.selected.item)) {
					selection.selected.setSelectedItem(null);
				}
				replaceSelectedItem();
				cleared = items.isEmpty();
				included.notifyChanges();
				excluded.notifyChanges();
			}
		}

		@Override
		public void add(T item) {
			validate(item);
			synchronized (lock) {
				if (included.predicate.test(item)) {
					if (!included.items.contains(item)) {
						//an item can linger in excluded if its stateful predicate flipped without predicate.set(),
						//remove it so it does not end up in both partitions
						boolean wasExcluded = excluded.items.remove(item);
						addNatural(item);
						if (wasExcluded) {
							//back at its natural position
							includeInternal();
						}
						else {
							//a new item, last in natural order
							included.items.add(item);
							included.sortInternal();
						}
						included.added.accept(singleton(item));
						included.notifyChanges();
						if (wasExcluded) {
							excluded.notifyChanges();
						}
					}
				}
				else if (!excluded.items.contains(item)) {
					boolean wasIncluded = included.items.remove(item);
					excluded.items.add(item);
					addNatural(item);
					excluded.notifyChanges();
					if (wasIncluded) {
						included.notifyChanges();
					}
				}
			}
		}

		@Override
		public void add(Collection<T> items) {
			// All validated before any is added, so a rejected item leaves the model as it was
			requireNonNull(items).forEach(this::validate);
			synchronized (lock) {
				for (T item : items) {
					add(item);
				}
			}
		}

		@Override
		public void remove(T item) {
			requireNonNull(item);
			synchronized (lock) {
				if (excluded.items.remove(item)) {
					natural.remove(item);
					excluded.notifyChanges();
				}
				else if (included.items.remove(item)) {
					natural.remove(item);
					included.notifyChanges();
					updateSelectedItem(item);
				}
			}
		}

		@Override
		public void remove(Collection<T> items) {
			rejectNulls(items);
			synchronized (lock) {
				for (T item : items) {
					remove(item);
				}
			}
		}

		@Override
		public void remove(Predicate<T> predicate) {
			requireNonNull(predicate);
			synchronized (lock) {
				remove(natural.stream()
								.filter(predicate)
								.collect(toList()));
			}
		}

		@Override
		public IncludedItems<T> included() {
			return included;
		}

		@Override
		public ExcludedItems<T> excluded() {
			return excluded;
		}

		@Override
		public boolean contains(T item) {
			synchronized (lock) {
				//the null item is no item, see nullable()
				return item != null && natural.contains(item);
			}
		}

		@Override
		public int size() {
			synchronized (lock) {
				return natural.size();
			}
		}

		@Override
		public void filter() {
			synchronized (lock) {
				if (size() > 0) {
					filterInternal();
					if (filterSelected
									&& selection.selected.item != null
									&& !included.items.contains(selection.selected.item)) {
						selection.selected.setSelectedItem(null);
					}
					included.notifyChanges();
					excluded.notifyChanges();
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
		}

		@Override
		public void replace(Map<T, T> items) {
			// Note: Similar logic exists in DefaultFilterModelItems.
			// Both implementations handle item replacement with filtering but have different collection types
			// and threading requirements, making extraction to a common utility non-trivial.
			requireNonNull(items).values().forEach(this::validate);
			synchronized (lock) {
				Map<T, T> replacements = new HashMap<>(items);
				items.forEach((item, replacement) -> {
					int index = natural.indexOf(item);
					if (index != -1) {
						natural.set(index, replacement);
					}
				});
				boolean reincluded = false;
				for (T itemToReplace : items.keySet()) {
					if (excluded.items.remove(itemToReplace)) {
						T replacement = replacements.remove(itemToReplace);
						if (included.predicate.test(replacement)) {
							//included below, at its natural position
							reincluded = true;
						}
						else {
							excluded.items.add(replacement);
						}
					}
				}
				ListIterator<T> iterator = included.items.listIterator();
				while (!replacements.isEmpty() && iterator.hasNext()) {
					T item = iterator.next();
					T replacement = replacements.remove(item);
					if (replacement != null) {
						if (included.predicate.test(replacement)) {
							iterator.set(replacement);
						}
						else {
							iterator.remove();
							excluded.items.add(replacement);
						}
					}
				}
				//update the selection if the selected item was among those replaced, and sort before notifying
				T selectedItem = selection.selected.item;
				if (selectedItem != null && items.containsKey(selectedItem)) {
					selection.selected.replaceWith(items.get(selectedItem));
				}
				if (reincluded) {
					includeInternal();
				}
				else {
					included.sortInternal();
				}
				included.notifyChanges();
				excluded.notifyChanges();
			}
		}

		@Override
		public boolean cleared() {
			return cleared;
		}

		@Override
		public boolean nullable() {
			return nullable;
		}

		@Override
		public @Nullable T nullItem() {
			return nullItem;
		}

		// Excludes the items not passing the predicate, in natural order, and includes the rest
		private void filterInternal() {
			excluded.items.clear();
			Predicate<T> predicate = included.predicate.get();
			if (predicate != null) {
				for (T item : natural) {
					if (!predicate.test(item)) {
						excluded.items.add(item);
					}
				}
			}
			includeInternal();
		}

		// The included items, the ones not excluded, in natural order, then sorted when sorting
		private void includeInternal() {
			included.items.clear();
			if (nullable) {
				included.items.add(null);
			}
			for (T item : natural) {
				if (!excluded.items.contains(item)) {
					included.items.add(item);
				}
			}
			included.sortInternal();
		}

		// A new item last in natural order, an existing one replaced in place, the given instance being the current one
		private void addNatural(T item) {
			int index = natural.indexOf(item);
			if (index == -1) {
				natural.add(item);
			}
			else {
				natural.set(index, item);
			}
		}

		private void updateSelectedItem(T removedItem) {
			if (Objects.equals(selection.selected.item, removedItem)) {
				if (modelItems.nullItem != null || nullable) {
					//select the null item / sentinel when the model includes null
					selection.selected.setSelectedItem(null);
				}
				else if (!modelItems.included.items.isEmpty()) {
					selection.selected.setSelectedItem(modelItems.included.items.get(0));
				}
				else {
					//no items remain and there is no null item, clear the now-stale selection
					selection.selected.setSelectedItem(null);
				}
			}
		}

		private void replaceSelectedItem() {
			if (selection.selected.item != null) {
				int index = included.items.indexOf(selection.selected.item);
				if (index != -1) {
					//replace the selected item since the underlying data could have changed
					selection.selected.replaceWith(included.items.get(index));
				}
			}
		}

		private void validate(T item) {
			if (!validator.test(requireNonNull(item))) {
				throw new IllegalArgumentException("Invalid item: " + item);
			}
		}

		private final class DefaultIncludedItems implements IncludedItems<T> {

			private final IncludePredicate<T> predicate = new DefaultIncludePredicate<>();
			private final List<@Nullable T> items = new ArrayList<>();
			private final Signal<List<T>> changed = Signal.signal();
			private final Signal<Collection<T>> added = Signal.signal();

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
					if (!nullable) {
						return unmodifiableList(new ArrayList<>(items));
					}

					return unmodifiableList(new ArrayList<>(items.subList(1, items.size())));
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
					//the null item is no item, see nullable()
					return false;
				}
				synchronized (lock) {
					return items.contains(item);
				}
			}

			@Override
			public int indexOf(T item) {
				synchronized (lock) {
					//the null item, first when nullable, has no index
					int index = items.indexOf(item);

					return nullable && index != -1 ? index - 1 : index;
				}
			}

			@Override
			public T get(int index) {
				synchronized (lock) {
					int size = size();
					if (index < 0 || index >= size) {
						throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
					}

					return items.get(nullable ? index + 1 : index);
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
					if (!nullable) {
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
					items.subList(nullable ? 1 : 0, items.size()).sort(sort);
					return true;
				}

				return false;
			}

			private void notifyChanges() {
				changed.accept(get());
			}
		}

		private final class DefaultExcludedItems implements ExcludedItems<T> {

			private final Signal<Collection<T>> changed = Signal.signal();
			private final Set<T> items = new LinkedHashSet<>();

			@Override
			public Collection<T> get() {
				synchronized (lock) {
					return unmodifiableList(new ArrayList<>(items));
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
		public ObservableState present() {
			return selected.present.observable();
		}

		@Override
		public Sensor changing() {
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

		private final Signal<T> changing = Signal.signal();
		private final State present = State.state();
		private final Function<@Nullable Object, T> translator;

		private @Nullable T item = null;

		private SelectedItem(Function<Object, T> translator) {
			this.translator = translator;
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
			// The selection may be an item that is not among the items, so it is validated in its own right
			if (toSelect != null && !Objects.equals(modelItems.nullItem, toSelect)) {
				modelItems.validate(toSelect);
			}
			if (!Objects.equals(this.item, toSelect)) {
				changing.accept(toSelect);
				this.item = toSelect;
				present.set(toSelect != null);
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
			if (selection.selected.present.is()) {
				return itemFinder.value(selection.item().getOrThrow());
			}

			return null;
		}

		@Override
		protected void setValue(@Nullable V value) {
			//search all items, not just included ones, since the selection tolerates excluded items (filterSelected=false)
			selection.selected.setSelectedItem(value == null ? null : itemFinder.findItem(modelItems.get(), value).orElse(null));
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

	static final class SelectedItemTranslator<T> implements Function<Object, Item<T>> {

		private final Map<T, Item<T>> itemMap;

		SelectedItemTranslator(List<Item<T>> items) {
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

	private interface Lock {}
}
