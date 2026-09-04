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
import java.util.List;
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
			selectedIndexes.set(rejectNulls(itemsToSelect).stream()
							.mapToInt(items::indexOf)
							.filter(index -> index >= 0)
							.boxed()
							.collect(toList()));
		}

		@Override
		public void set(Predicate<R> predicate) {
			selectedIndexes.set(indexesToSelect(requireNonNull(predicate)));
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
			selectedIndexes.remove(rejectNulls(itemsToRemove).stream()
							.mapToInt(items::indexOf)
							.filter(index -> index >= 0)
							.boxed()
							.collect(toList()));
		}

		@Override
		public boolean contains(R item) {
			return isSelectedIndex(items.indexOf(requireNonNull(item)));
		}

		@Override
		public Optional<List<R>> optional() {
			List<R> selectedItemList = getOrThrow();

			return selectedItemList.isEmpty() ? Optional.empty() : Optional.of(selectedItemList);
		}

		private void addInternal(Collection<R> itemsToAdd) {
			selectedIndexes.add(itemsToAdd.stream()
							.mapToInt(items::indexOf)
							.filter(index -> index >= 0)
							.boxed()
							.collect(toList()));
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
			changing.run();
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
