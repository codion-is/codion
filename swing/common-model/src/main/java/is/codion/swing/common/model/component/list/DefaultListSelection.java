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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.list;

import is.codion.common.event.Event;
import is.codion.common.model.filter.FilterModel.VisibleItems;
import is.codion.common.observable.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;

import org.jspecify.annotations.Nullable;

import javax.swing.DefaultListSelectionModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultListSelection<R> extends DefaultListSelectionModel implements FilterListSelection<R> {

	private final SelectedIndex selectedIndex = new SelectedIndex();
	private final SelectedIndexes selectedIndexes = new SelectedIndexes();
	private final DefaultItem selectedItem = new DefaultItem();
	private final DefaultItems selectedItems = new DefaultItems();
	private final Event<?> changing = Event.event();
	private final State singleSelection = State.state(false);
	private final State empty = State.state(true);
	private final State single = State.state(false);
	private final ObservableState multiple = State.and(empty.not(), single.not());

	private final VisibleItems<R> items;

	DefaultListSelection(VisibleItems<R> items) {
		this.items = requireNonNull(items);
		bindEvents();
	}

	@Override
	public void setSelectionMode(int selectionMode) {
		if (getSelectionMode() != selectionMode) {
			clear();
			super.setSelectionMode(selectionMode);
			singleSelection.set(selectionMode == SINGLE_SELECTION);
		}
	}

	@Override
	public State singleSelection() {
		return singleSelection;
	}

	@Override
	public int count() {
		if (isSelectionEmpty()) {
			return 0;
		}

		return (int) IntStream.rangeClosed(getMinSelectionIndex(), getMaxSelectionIndex())
						.filter(this::isSelectedIndex).count();
	}

	@Override
	public void adjusting(boolean adjusting) {
		setValueIsAdjusting(adjusting);
	}

	@Override
	public Indexes indexes() {
		return selectedIndexes;
	}

	@Override
	public Value<Integer> index() {
		return selectedIndex;
	}

	@Override
	public void selectAll() {
		setSelectionInterval(0, items.count() - 1);
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
	public void addSelectionInterval(int fromIndex, int toIndex) {
		changing.run();
		super.addSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void setSelectionInterval(int fromIndex, int toIndex) {
		changing.run();
		super.setSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void removeSelectionInterval(int fromIndex, int toIndex) {
		changing.run();
		super.removeSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void insertIndexInterval(int fromIndex, int length, boolean before) {
		changing.run();
		super.insertIndexInterval(fromIndex, length, before);
	}

	@Override
	public void removeIndexInterval(int fromIndex, int toIndex) {
		changing.run();
		super.removeIndexInterval(fromIndex, toIndex);
	}

	@Override
	public Observer<?> changing() {
		return changing.observer();
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
	public void clear() {
		clearSelection();
	}

	@Override
	protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
		super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
		if (!isAdjusting) {
			empty.set(super.isSelectionEmpty());
			single.set(count() == 1);
			selectedIndex.onChanged();
			selectedItem.onChanged();
			selectedIndexes.onChanged();
			selectedItems.onChanged();
		}
	}

	private void bindEvents() {
		singleSelection.addConsumer(singleSelectionMode ->
						setSelectionMode(singleSelectionMode ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION));
	}

	private static void checkIndex(int index, int size) {
		if (index < 0 || index > size - 1) {
			throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
		}
	}

	private final class SelectedIndex extends AbstractValue<Integer> {

		private SelectedIndex() {
			super(-1);
		}

		@Override
		protected Integer getValue() {
			return getMinSelectionIndex();
		}

		@Override
		protected void setValue(Integer index) {
			checkIndex(index, items.count());
			setSelectionInterval(index, index);
		}

		private void onChanged() {
			notifyListeners();
		}
	}

	private final class SelectedIndexes extends AbstractValue<List<Integer>> implements Indexes {

		private SelectedIndexes() {
			super(emptyList());
		}

		@Override
		protected List<Integer> getValue() {
			if (isSelectionEmpty()) {
				return emptyList();
			}

			return unmodifiableList(IntStream.rangeClosed(getMinSelectionIndex(), getMaxSelectionIndex())
							.filter(DefaultListSelection.this::isSelectedIndex)
							.boxed()
							.collect(toList()));
		}

		@Override
		protected void setValue(List<Integer> indexes) {
			checkIndexes(indexes);
			setValueIsAdjusting(true);
			clearSelection();
			add(indexes);
			setValueIsAdjusting(false);
		}

		@Override
		public void add(int index) {
			checkIndex(index, items.count());
			addSelectionInterval(index, index);
		}

		@Override
		public void remove(int index) {
			checkIndex(index, items.count());
			removeSelectionInterval(index, index);
		}

		@Override
		public void remove(Collection<Integer> indexes) {
			indexes.forEach(index -> {
				checkIndex(index, items.count());
				removeSelectionInterval(index, index);
			});
		}

		@Override
		public void add(Collection<Integer> indexes) {
			if (requireNonNull(indexes).isEmpty()) {
				return;
			}
			checkIndexes(indexes);
			setValueIsAdjusting(true);
			for (Integer index : indexes) {
				addSelectionInterval(index, index);
			}
			setValueIsAdjusting(false);
		}

		@Override
		public boolean contains(int index) {
			return isSelectedIndex(index);
		}

		@Override
		public void decrement() {
			int visibleSize = items.count();
			if (visibleSize > 0) {
				int lastIndex = visibleSize - 1;
				if (isSelectionEmpty()) {
					setSelectionInterval(lastIndex, lastIndex);
				}
				else {
					selectedIndexes.set(selectedIndexes.get().stream()
									.map(index -> index == 0 ? lastIndex : index - 1)
									.collect(toList()));
				}
			}
		}

		@Override
		public void increment() {
			int filteredSize = items.count();
			if (filteredSize > 0) {
				if (isSelectionEmpty()) {
					setSelectionInterval(0, 0);
				}
				else {
					selectedIndexes.set(selectedIndexes.get().stream()
									.map(index -> index == filteredSize - 1 ? 0 : index + 1)
									.collect(toList()));
				}
			}
		}

		private void checkIndexes(Collection<Integer> indexes) {
			int size = items.count();
			for (Integer index : indexes) {
				checkIndex(index, size);
			}
		}

		void onChanged() {
			notifyListeners();
		}
	}

	private final class DefaultItem extends AbstractValue<R> {

		@Override
		protected @Nullable R getValue() {
			int index = selectedIndex.getOrThrow();
			if (index >= 0 && index < items.count()) {
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
			notifyListeners();
		}
	}

	private final class DefaultItems extends AbstractValue<List<R>> implements Items<R> {

		private DefaultItems() {
			super(emptyList());
		}

		@Override
		protected List<R> getValue() {
			return unmodifiableList(selectedIndexes.getOrThrow().stream()
							.mapToInt(Integer::intValue)
							.mapToObj(items::get)
							.collect(toList()));
		}

		@Override
		public void set(Collection<R> items) {
			setValue(new ArrayList<>(requireNonNull(items)));
		}

		@Override
		protected void setValue(List<R> items) {
			rejectNulls(items);
			clearSelection();
			addInternal(items);
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
		public void add(Collection<R> items) {
			rejectNulls(items);
			addInternal(items);
		}

		@Override
		public void remove(R item) {
			remove(singletonList(requireNonNull(item)));
		}

		@Override
		public void remove(Collection<R> itemsToRemove) {
			rejectNulls(itemsToRemove).forEach(item -> selectedIndexes.remove(items.indexOf(item)));
		}

		@Override
		public boolean contains(R item) {
			return isSelectedIndex(items.indexOf(requireNonNull(item)));
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
			List<R> visibleItems = items.get();
			for (int i = 0; i < visibleItems.size(); i++) {
				R item = visibleItems.get(i);
				if (predicate.test(item)) {
					indexes.add(i);
				}
			}

			return indexes;
		}

		private void onChanged() {
			notifyListeners();
		}

		private <T> Collection<T> rejectNulls(Collection<T> items) {
			for (T item : requireNonNull(items)) {
				requireNonNull(item);
			}

			return items;
		}
	}
}
