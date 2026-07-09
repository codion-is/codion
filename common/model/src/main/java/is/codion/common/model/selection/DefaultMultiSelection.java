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
import java.util.TreeSet;
import java.util.function.Predicate;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A pure-Java {@link MultiSelection} implementation backed by a {@link NavigableSet} of selected
 * indexes — the AWT/Swing-free counterpart to {@code DefaultListSelection}, which borrows
 * {@code javax.swing.DefaultListSelectionModel} as its engine.
 *
 * <p>A Compose implementation detail for now. If a second non-Swing client appears, this (with its
 * test) is a prime candidate for promotion into {@code codion-common-model} alongside
 * {@code MultiSelection}/{@code SingleSelection}.
 */
final class DefaultMultiSelection<R> implements MultiSelection<R> {

	private final IndexedItems<R> items;
	private final NavigableSet<Integer> selected = new TreeSet<>();

	private final SelectedIndex selectedIndex = new SelectedIndex();
	private final SelectedIndexes selectedIndexes = new SelectedIndexes();
	private final SelectedItem selectedItem = new SelectedItem();
	private final SelectedItems selectedItems = new SelectedItems();
	private final Event<?> changing = Event.event();
	private final State singleSelection = State.state(false);
	private final State empty = State.state(true);
	private final State single = State.state(false);
	private final ObservableState multiple = State.and(empty.not(), single.not());

	private boolean adjusting = false;

	DefaultMultiSelection(IndexedItems<R> items) {
		this.items = items;
		this.singleSelection.addConsumer(this::onSingleSelectionChanged);
	}

	@Override
	public State singleSelection() {
		return singleSelection;
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
	public ObservableState empty() {
		return empty.observable();
	}

	@Override
	public Observer<?> changing() {
		return changing.observer();
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
		return selected.size();
	}

	@Override
	public void selectAll() {
		if (items.size() > 0) {
			setSelectionInterval(0, items.size() - 1);
		}
	}

	@Override
	public void adjusting(boolean adjusting) {
		setAdjusting(adjusting);
	}

	@Override
	public void clear() {
		clearSelection();
	}

	private void setSelectionInterval(int fromIndex, int toIndex) {
		applyTarget(range(fromIndex, toIndex));
	}

	private void clearSelection() {
		applyTarget(new TreeSet<>());
	}

	/**
	 * Applies the target selection: enforces single-selection mode, and — crucially — skips the
	 * change notification entirely when the selection does not actually change. Selection models
	 * must stay silent on no-ops; the framework's selection↔editor linking loops infinitely otherwise.
	 */
	private void applyTarget(NavigableSet<Integer> target) {
		if (singleSelection.is() && target.size() > 1) {
			int keep = target.last();
			target.clear();
			target.add(keep);
		}
		if (target.equals(selected)) {
			return;
		}
		changing.run();
		selected.clear();
		selected.addAll(target);
		changed();
	}

	private static NavigableSet<Integer> range(int fromIndex, int toIndex) {
		NavigableSet<Integer> range = new TreeSet<>();
		for (int i = Math.min(fromIndex, toIndex); i <= Math.max(fromIndex, toIndex); i++) {
			range.add(i);
		}

		return range;
	}

	private void setAdjusting(boolean adjusting) {
		this.adjusting = adjusting;
		if (!adjusting) {
			changed();
		}
	}

	private void changed() {
		if (!adjusting) {
			empty.set(selected.isEmpty());
			single.set(selected.size() == 1);
			selectedIndex.onChanged();
			selectedItem.onChanged();
			selectedIndexes.onChanged();
			selectedItems.onChanged();
		}
	}

	private void onSingleSelectionChanged(boolean singleSelectionMode) {
		clearSelection(); // mirror Swing: changing selection mode clears the selection
	}

	private boolean isSelectedIndex(int index) {
		return selected.contains(index);
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
			int index = minSelectionIndex();

			return index == -1 ? null : index;
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

		private int minSelectionIndex() {
			return selected.isEmpty() ? -1 : selected.first();
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
			return unmodifiableList(new ArrayList<>(selected));
		}

		@Override
		protected void setValue(List<Integer> indexes) {
			checkIndexes(indexes);
			applyTarget(new TreeSet<>(indexes));
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
			NavigableSet<Integer> target = new TreeSet<>(selected);
			target.addAll(indexes);
			applyTarget(target);
		}

		@Override
		public void remove(Collection<Integer> indexes) {
			if (requireNonNull(indexes).isEmpty()) {
				return;
			}
			checkIndexes(indexes);
			NavigableSet<Integer> target = new TreeSet<>(selected);
			target.removeAll(indexes);
			applyTarget(target);
		}

		@Override
		public boolean contains(int index) {
			return isSelectedIndex(index);
		}

		@Override
		public void increment() {
			int size = items.size();
			if (size > 0) {
				if (selected.isEmpty()) {
					setSelectionInterval(0, 0);
				}
				else {
					set(get().stream()
									.map(index -> index == size - 1 ? 0 : index + 1)
									.collect(toList()));
				}
			}
		}

		@Override
		public void decrement() {
			int size = items.size();
			if (size > 0) {
				int lastIndex = size - 1;
				if (selected.isEmpty()) {
					setSelectionInterval(lastIndex, lastIndex);
				}
				else {
					set(get().stream()
									.map(index -> index == 0 ? lastIndex : index - 1)
									.collect(toList()));
				}
			}
		}

		@Override
		public Optional<List<Integer>> optional() {
			List<Integer> indexes = get();

			return indexes.isEmpty() ? Optional.empty() : Optional.of(indexes);
		}

		private void addSelectionInterval(int fromIndex, int toIndex) {
			NavigableSet<Integer> target = new TreeSet<>(selected);
			target.addAll(range(fromIndex, toIndex));
			applyTarget(target);
		}

		private void removeSelectionInterval(int fromIndex, int toIndex) {
			NavigableSet<Integer> target = new TreeSet<>(selected);
			target.removeAll(range(fromIndex, toIndex));
			applyTarget(target);
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
			R current = getValue();
			if (!Objects.equals(lastNotified, current)) {
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
			return unmodifiableList(selected.stream()
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
			rejectNulls(itemsToSelect);
			selectedIndexes.set(itemsToSelect.stream()
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
			rejectNulls(itemsToAdd);
			addInternal(itemsToAdd);
		}

		@Override
		public void remove(R item) {
			remove(singletonList(requireNonNull(item)));
		}

		@Override
		public void remove(Collection<R> itemsToRemove) {
			rejectNulls(itemsToRemove);
			selectedIndexes.remove(itemsToRemove.stream()
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
			List<R> selectedItemList = get();

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
			List<R> current = getValue();
			if (!lastNotified.equals(current)) {
				lastNotified = current;
				notifyObserver();
			}
		}
	}

	private static <T> void rejectNulls(Collection<T> items) {
		for (T item : requireNonNull(items)) {
			requireNonNull(item);
		}
	}
}
