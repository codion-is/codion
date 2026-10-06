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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.selection;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import static is.codion.common.utilities.Nulls.rejectNulls;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * The default {@link MultiSelection} implementation, its index and item facades a view over a {@link IndexStore}, by
 * default {@link DefaultIndexStore}, a {@link NavigableSet} of selected indexes, or a toolkit's own, such as the
 * {@code javax.swing.DefaultListSelectionModel} the Swing selection keeps for its {@code JTable}.
 *
 * <p>Note that the {@link #index()}, {@link #indexes()}, {@link #item()} and {@link #items()} facades
 * use the non-notifying {@link is.codion.common.reactive.value.AbstractValue} constructors; notification
 * is owned by their {@code onChanged()}, which fires only when the facade's own value actually changes.
 * A selection model must stay silent on a no-op, the framework's selection to editor linking loops
 * infinitely otherwise.
 */
final class DefaultMultiSelection<R> implements MultiSelection<R> {

	//the number of items to find below which comparing each item with them beats hashing each item
	private static final int FEW = 8;

	private final IndexedItems<R> items;
	private final IndexStore store;

	private final SelectedIndex selectedIndex = new SelectedIndex();
	private final SelectedIndexes selectedIndexes = new SelectedIndexes();
	private final SelectedItem selectedItem = new SelectedItem();
	private final SelectedItems selectedItems = new SelectedItems();
	private final State present = State.state();
	private final State single = State.state();
	private final ObservableState multiple = State.and(present, single.not());

	DefaultMultiSelection(IndexedItems<R> items) {
		this(items, new DefaultIndexStore());
	}

	DefaultMultiSelection(IndexedItems<R> items, IndexStore store) {
		this.items = requireNonNull(items);
		this.store = requireNonNull(store);
		this.store.changed().addListener(this::onChanged);
	}

	@Override
	public State singleSelection() {
		return store.singleSelection();
	}

	@Override
	public ObservableState multiple() {
		return multiple;
	}

	@Override
	public ObservableState single() {
		return single.observable();
	}

	@Override
	public ObservableState present() {
		return present.observable();
	}

	@Override
	public Observer<?> changing() {
		return store.changing();
	}

	@Override
	public Value<Integer> index() {
		return selectedIndex;
	}

	@Override
	public Indexes indexes() {
		return selectedIndexes;
	}

	@Override
	public Value<R> item() {
		return selectedItem;
	}

	@Override
	public Items<R> items() {
		return selectedItems;
	}

	@Override
	public int count() {
		return store.size();
	}

	@Override
	public void selectAll() {
		if (items.size() > 0) {
			setSelectionInterval(0, items.size() - 1);
		}
	}

	@Override
	public Grouping grouping() {
		return store.grouping();
	}

	@Override
	public Observer<?> adjusting() {
		return store.adjusting();
	}

	@Override
	public void clear() {
		clearSelection();
	}

	private void setSelectionInterval(int fromIndex, int toIndex) {
		store.set(range(fromIndex, toIndex));
	}

	private void clearSelection() {
		store.set(emptySet());
	}

	private static NavigableSet<Integer> range(int fromIndex, int toIndex) {
		NavigableSet<Integer> range = new TreeSet<>();
		for (int i = Math.min(fromIndex, toIndex); i <= Math.max(fromIndex, toIndex); i++) {
			range.add(i);
		}

		return range;
	}

	private void onChanged() {
		Set<Integer> selected = store.get();
		present.set(!selected.isEmpty());
		single.set(selected.size() == 1);
		selectedIndex.onChanged();
		selectedItem.onChanged();
		selectedIndexes.onChanged();
		selectedItems.onChanged();
	}

	private boolean isSelectedIndex(int index) {
		return store.contains(index);
	}

	private static void checkIndex(int index, int size) {
		if (index < 0 || index > size - 1) {
			throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
		}
	}

	private final class SelectedIndex extends AbstractValue<Integer> {

		private @Nullable Integer lastNotified;

		@Override
		protected @Nullable Integer getValue() {
			return store.size() == 0 ? null : store.get().iterator().next();
		}

		@Override
		protected void setValue(@Nullable Integer index) {
			if (index == null) {
				clearSelection();
			}
			else {
				checkIndex(index, items.size());
				setSelectionInterval(index, index);
			}
		}

		private void onChanged() {
			//only notify when this facade's value actually changed, honoring the Notify.CHANGED contract
			Integer current = getValue();
			if (!Objects.equals(lastNotified, current)) {
				lastNotified = current;
				notifyObserver();
			}
		}
	}

	private final class SelectedIndexes extends AbstractValue<List<Integer>> implements Indexes {

		private List<Integer> lastNotified = emptyList();

		private SelectedIndexes() {
			super(emptyList());
		}

		@Override
		protected List<Integer> getValue() {
			return unmodifiableList(new ArrayList<>(store.get()));
		}

		@Override
		protected void setValue(List<Integer> indexes) {
			checkIndexes(indexes);
			store.set(indexes);
		}

		@Override
		public void add(int index) {
			checkIndex(index, items.size());
			addSelectionInterval(index, index);
		}

		@Override
		public void remove(int index) {
			checkIndex(index, items.size());
			removeSelectionInterval(index, index);
		}

		@Override
		public void add(Collection<Integer> indexes) {
			if (requireNonNull(indexes).isEmpty()) {
				return;
			}
			checkIndexes(indexes);
			NavigableSet<Integer> target = new TreeSet<>(store.get());
			target.addAll(indexes);
			store.set(target);
		}

		@Override
		public void remove(Collection<Integer> indexes) {
			if (requireNonNull(indexes).isEmpty()) {
				return;
			}
			checkIndexes(indexes);
			NavigableSet<Integer> target = new TreeSet<>(store.get());
			target.removeAll(indexes);
			store.set(target);
		}

		@Override
		public boolean contains(int index) {
			return isSelectedIndex(index);
		}

		@Override
		public void increment() {
			int size = items.size();
			if (size > 0) {
				if (store.size() == 0) {
					setSelectionInterval(0, 0);
				}
				else {
					int lastIndex = size - 1;
					set(getOrThrow().stream()
									.map(index -> index == lastIndex ? 0 : index + 1)
									.collect(toList()));
				}
			}
		}

		@Override
		public void decrement() {
			int size = items.size();
			if (size > 0) {
				int lastIndex = size - 1;
				if (store.size() == 0) {
					setSelectionInterval(lastIndex, lastIndex);
				}
				else {
					set(getOrThrow().stream()
									.map(index -> index == 0 ? lastIndex : index - 1)
									.collect(toList()));
				}
			}
		}

		@Override
		public Optional<List<Integer>> optional() {
			List<Integer> indexes = getOrThrow();

			return indexes.isEmpty() ? Optional.empty() : Optional.of(indexes);
		}

		private void addSelectionInterval(int fromIndex, int toIndex) {
			NavigableSet<Integer> target = new TreeSet<>(store.get());
			target.addAll(range(fromIndex, toIndex));
			store.set(target);
		}

		private void removeSelectionInterval(int fromIndex, int toIndex) {
			NavigableSet<Integer> target = new TreeSet<>(store.get());
			target.removeAll(range(fromIndex, toIndex));
			store.set(target);
		}

		private void checkIndexes(Collection<Integer> indexes) {
			int size = items.size();
			for (Integer index : indexes) {
				checkIndex(index, size);
			}
		}

		private void onChanged() {
			List<Integer> current = getValue();
			if (!lastNotified.equals(current)) {
				lastNotified = current;
				notifyObserver();
			}
		}
	}

	private final class SelectedItem extends AbstractValue<R> {

		private @Nullable R lastNotified;

		@Override
		protected @Nullable R getValue() {
			Integer index = selectedIndex.get();
			if (index != null && index < items.size()) {
				return items.get(index);
			}

			return null;
		}

		@Override
		protected void setValue(@Nullable R item) {
			if (item == null) {
				clearSelection();
			}
			else {
				selectedItems.set(singletonList(item));
			}
		}

		private void onChanged() {
			//by identity rather than equals(): the items model owns the instances and replaces them on refresh and
			//replace(), a replacement being the same item by equals() but a new value of this facade
			R current = getValue();
			if (lastNotified != current) {
				lastNotified = current;
				notifyObserver();
			}
		}
	}

	private final class SelectedItems extends AbstractValue<List<R>> implements Items<R> {

		private List<R> lastNotified = emptyList();

		private SelectedItems() {
			super(emptyList());
		}

		@Override
		protected List<R> getValue() {
			return unmodifiableList(store.get().stream()
							.filter(index -> index < items.size())
							.map(items::get)
							.collect(toList()));
		}

		@Override
		public void set(Collection<R> itemsToSelect) {
			setValue(new ArrayList<>(requireNonNull(itemsToSelect)));
		}

		@Override
		protected void setValue(List<R> itemsToSelect) {
			selectedIndexes.set(indexesOf(rejectNulls(itemsToSelect), true));
		}

		@Override
		public void set(Predicate<R> predicate) {
			selectedIndexes.set(indexesToSelect(requireNonNull(predicate)));
		}

		@Override
		public void restore(Collection<R> itemsToRestore) {
			//not preferring the selected indexes, which a view may have shifted or cleared while the items changed
			store.restore(indexesOf(rejectNulls(itemsToRestore), false));
		}

		@Override
		public void add(Predicate<R> predicate) {
			selectedIndexes.add(indexesToSelect(requireNonNull(predicate)));
		}

		@Override
		public void add(R item) {
			addInternal(singletonList(requireNonNull(item)));
		}

		@Override
		public void add(Collection<R> itemsToAdd) {
			addInternal(rejectNulls(itemsToAdd));
		}

		@Override
		public void remove(R item) {
			remove(singletonList(requireNonNull(item)));
		}

		@Override
		public void remove(Collection<R> itemsToRemove) {
			//every selected index holding an equal item, not only the first equal item's index
			Collection<R> toRemove = rejectNulls(itemsToRemove);
			selectedIndexes.remove(store.get().stream()
							.filter(index -> index < items.size() && toRemove.contains(items.get(index)))
							.collect(toList()));
		}

		@Override
		public boolean contains(R item) {
			requireNonNull(item);
			//any selected index holding an equal item, not only the first equal item's index
			return store.get().stream()
							.anyMatch(index -> index < items.size() && item.equals(items.get(index)));
		}

		@Override
		public Optional<List<R>> optional() {
			List<R> selectedItemList = getOrThrow();

			return selectedItemList.isEmpty() ? Optional.empty() : Optional.of(selectedItemList);
		}

		private void addInternal(Collection<R> itemsToAdd) {
			selectedIndexes.add(indexesOf(itemsToAdd, true));
		}

		/**
		 * Returns a distinct index for each of the given items, in order of preference: the index holding the same
		 * instance, a selected one in case of {@code preferSelected}, then any, otherwise an index holding an equal item,
		 * a selected one in case of {@code preferSelected}, then the first one, the items not found left out.
		 * @param itemsToFind the items to find
		 * @param preferSelected true if selected indexes should be preferred
		 * @return the indexes of the given items
		 */
		private List<Integer> indexesOf(Collection<R> itemsToFind, boolean preferSelected) {
			if (itemsToFind.isEmpty()) {
				return emptyList();
			}
			List<R> toFind = new ArrayList<>(itemsToFind);
			if (toFind.size() <= FEW && (!preferSelected || noneSelected(toFind))) {
				List<Integer> indexes = firstSameInstances(toFind);
				if (indexes != null) {
					return indexes;
				}
			}
			List<R> current = items.get();
			//the indexes holding an item equal to one of the given ones, in ascending order, comparing a few
			//items with each one, rather than hashing each one, as most selections are a single item
			List<R> distinct = new ArrayList<>(new LinkedHashSet<>(toFind));
			Set<R> wanted = distinct.size() > FEW ? new HashSet<>(distinct) : null;
			Map<R, List<Integer>> candidates = new HashMap<>();
			for (int index = 0; index < current.size(); index++) {
				R item = current.get(index);
				R found = wanted == null ? equal(distinct, item) : wanted.contains(item) ? item : null;
				if (found != null) {
					candidates.computeIfAbsent(found, k -> new ArrayList<>()).add(index);
				}
			}
			Set<Integer> selected = preferSelected ? store.get() : emptySet();
			boolean[] found = new boolean[toFind.size()];
			Set<Integer> indexes = new LinkedHashSet<>();
			//each preference in turn, for all the items, before the next one
			for (int preference = 0; preference < 4; preference++) {
				boolean sameInstance = preference < 2;
				boolean selectedOnly = preference % 2 == 0;
				for (int i = 0; i < toFind.size(); i++) {
					if (!found[i]) {
						R item = toFind.get(i);
						for (Integer index : candidates.getOrDefault(item, emptyList())) {
							if (!indexes.contains(index)
											&& (!sameInstance || current.get(index) == item)
											&& (!selectedOnly || selected.contains(index))) {
								indexes.add(index);
								found[i] = true;
								break;
							}
						}
					}
				}
			}

			return new ArrayList<>(indexes);
		}

		/**
		 * @return true if no selected index holds an item equal to one of the given ones, preferring the selected
		 * indexes then making no difference
		 */
		private boolean noneSelected(List<R> itemsToFind) {
			for (Integer index : store.get()) {
				if (index < items.size() && equal(itemsToFind, items.get(index)) != null) {
					return false;
				}
			}

			return true;
		}

		/**
		 * The common case, a few items, each the first one equal to it, being the same instance, with no selected index
		 * holding an equal item to prefer, in which case these are the indexes the full search arrives at, without
		 * copying and searching all the items, as moving them, by sorting or filtering, keeps the instances.
		 * @return the first index of each of the given items, in case each holds the same instance, otherwise null
		 */
		private @Nullable List<Integer> firstSameInstances(List<R> itemsToFind) {
			List<Integer> indexes = new ArrayList<>(itemsToFind.size());
			for (R item : itemsToFind) {
				int index = items.indexOf(item);
				if (index < 0 || items.get(index) != item || indexes.contains(index)) {
					return null;
				}
				indexes.add(index);
			}

			return indexes;
		}

		private @Nullable R equal(List<R> itemsToFind, R item) {
			for (R itemToFind : itemsToFind) {
				if (itemToFind == item || itemToFind.equals(item)) {
					return itemToFind;
				}
			}

			return null;
		}

		private List<Integer> indexesToSelect(Predicate<R> predicate) {
			List<Integer> indexes = new ArrayList<>();
			List<R> included = items.get();
			for (int i = 0; i < included.size(); i++) {
				if (predicate.test(included.get(i))) {
					indexes.add(i);
				}
			}

			return indexes;
		}

		private void onChanged() {
			//by identity, see SelectedItem
			List<R> current = getValue();
			if (!sameInstances(lastNotified, current)) {
				lastNotified = current;
				notifyObserver();
			}
		}
	}

	/**
	 * The default {@link IndexStore}, the selected indexes in a {@link TreeSet}.
	 */
	private static final class DefaultIndexStore implements IndexStore {

		private final NavigableSet<Integer> selected = new TreeSet<>();
		private final Event<?> changing = Event.event();
		private final Event<?> changed = Event.event();
		private final Event<?> adjusting = Event.event();
		private final State singleSelection = State.state(false);
		private final DefaultGrouping grouping = new DefaultGrouping();

		private DefaultIndexStore() {
			singleSelection.addListener(() -> set(emptySet())); // mirror Swing: changing selection mode clears the selection
		}

		@Override
		public Set<Integer> get() {
			return unmodifiableSet(new TreeSet<>(selected));
		}

		@Override
		public void set(Collection<Integer> indexes) {
			set(indexes, true);
		}

		@Override
		public void restore(Collection<Integer> indexes) {
			set(indexes, false);
		}

		private void set(Collection<Integer> indexes, boolean notifyChanging) {
			NavigableSet<Integer> target = new TreeSet<>(indexes);
			if (singleSelection.is() && target.size() > 1) {
				Integer keep = target.last();
				target.clear();
				target.add(keep);
			}
			if (target.equals(selected)) {
				//silent on a no-op, the framework's selection to editor linking loops infinitely otherwise
				return;
			}
			if (notifyChanging) {
				changing.run();
			}
			selected.clear();
			selected.addAll(target);
			adjusting.run();
			if (!grouping.is()) {
				changed.run();
			}
		}

		@Override
		public int size() {
			return selected.size();
		}

		@Override
		public boolean contains(int index) {
			return selected.contains(index);
		}

		@Override
		public State singleSelection() {
			return singleSelection;
		}

		@Override
		public Grouping grouping() {
			return grouping;
		}

		@Override
		public Observer<?> changing() {
			return changing.observer();
		}

		@Override
		public Observer<?> changed() {
			return changed.observer();
		}

		@Override
		public Observer<?> adjusting() {
			return adjusting;
		}

		private final class DefaultGrouping implements Grouping {

			private boolean grouping = false;

			@Override
			public void set(boolean grouping) {
				boolean ended = this.grouping && !grouping;
				this.grouping = grouping;
				if (ended) {
					changed.run();
				}
			}

			@Override
			public boolean is() {
				return grouping;
			}
		}
	}

	private static <T> boolean sameInstances(List<T> first, List<T> second) {
		if (first.size() != second.size()) {
			return false;
		}
		for (int i = 0; i < first.size(); i++) {
			if (first.get(i) != second.get(i)) {
				return false;
			}
		}

		return true;
	}
}
