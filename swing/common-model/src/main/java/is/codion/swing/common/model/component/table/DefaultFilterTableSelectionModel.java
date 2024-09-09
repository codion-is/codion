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
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.observable.Observable;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultFilterTableSelectionModel<R> implements FilterTableSelectionModel<R> {

	private final FilterListSelectionModel selectionModel = new FilterListSelectionModel();
	private final SelectedIndex selectedIndex = new SelectedIndex();
	private final DefaultSelectedIndexes selectedIndexes = new DefaultSelectedIndexes();
	private final SelectedItem selectedItem = new SelectedItem();
	private final DefaultSelectedItems selectedItems = new DefaultSelectedItems();
	private final Event<?> selectionChanging = Event.event();
	private final State singleSelectionMode = State.state(false);
	private final State selectionEmpty = State.state(true);
	private final State singleSelection = State.state(false);
	private final StateObserver multipleSelection = State.and(selectionEmpty.not(), singleSelection.not());

	private final FilterTableModel<R, ?> tableModel;

	DefaultFilterTableSelectionModel(FilterTableModel<R, ?> tableModel) {
		this.tableModel = requireNonNull(tableModel, "tableModel");
		bindEvents();
	}

	@Override
	public void setSelectionMode(int selectionMode) {
		if (getSelectionMode() != selectionMode) {
			clearSelection();
			selectionModel.setSelectionMode(selectionMode);
			singleSelectionMode.set(selectionMode == SINGLE_SELECTION);
		}
	}

	@Override
	public State singleSelectionMode() {
		return singleSelectionMode;
	}

	@Override
	public int selectionCount() {
		if (isSelectionEmpty()) {
			return 0;
		}

		return (int) IntStream.rangeClosed(getMinSelectionIndex(), getMaxSelectionIndex())
						.filter(this::isSelectedIndex).count();
	}

	@Override
	public SelectedIndexes selectedIndexes() {
		return selectedIndexes;
	}

	@Override
	public Observable<Integer> selectedIndex() {
		return selectedIndex;
	}

	@Override
	public void selectAll() {
		setSelectionInterval(0, tableModel.visibleCount() - 1);
	}

	@Override
	public Observable<R> selectedItem() {
		return selectedItem;
	}

	@Override
	public SelectedItems<R> selectedItems() {
		return selectedItems;
	}

	@Override
	public boolean isSelected(R item) {
		requireNonNull(item);

		return isSelectedIndex(tableModel.indexOf(item));
	}

	@Override
	public void addSelectionInterval(int fromIndex, int toIndex) {
		selectionChanging.run();
		selectionModel.addSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void setSelectionInterval(int fromIndex, int toIndex) {
		selectionChanging.run();
		selectionModel.setSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void removeSelectionInterval(int fromIndex, int toIndex) {
		selectionChanging.run();
		selectionModel.removeSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void insertIndexInterval(int fromIndex, int length, boolean before) {
		selectionChanging.run();
		selectionModel.insertIndexInterval(fromIndex, length, before);
	}

	@Override
	public void removeIndexInterval(int fromIndex, int toIndex) {
		selectionChanging.run();
		selectionModel.removeIndexInterval(fromIndex, toIndex);
	}

	@Override
	public EventObserver<?> selectionChanging() {
		return selectionChanging.observer();
	}

	@Override
	public StateObserver multipleSelection() {
		return multipleSelection;
	}

	@Override
	public StateObserver singleSelection() {
		return singleSelection.observer();
	}

	@Override
	public StateObserver selectionEmpty() {
		return selectionEmpty.observer();
	}

	@Override
	public StateObserver selectionNotEmpty() {
		return selectionEmpty.not();
	}

	@Override
	public int getMinSelectionIndex() {
		return selectionModel.getMinSelectionIndex();
	}

	@Override
	public int getMaxSelectionIndex() {
		return selectionModel.getMaxSelectionIndex();
	}

	@Override
	public boolean isSelectedIndex(int index) {
		return selectionModel.isSelectedIndex(index);
	}

	@Override
	public int getAnchorSelectionIndex() {
		return selectionModel.getAnchorSelectionIndex();
	}

	@Override
	public void setAnchorSelectionIndex(int index) {
		selectionModel.setAnchorSelectionIndex(index);
	}

	@Override
	public int getLeadSelectionIndex() {
		return selectionModel.getLeadSelectionIndex();
	}

	@Override
	public void setLeadSelectionIndex(int index) {
		selectionModel.setLeadSelectionIndex(index);
	}

	@Override
	public void clearSelection() {
		selectionModel.clearSelection();
	}

	@Override
	public boolean isSelectionEmpty() {
		return selectionModel.isSelectionEmpty();
	}

	@Override
	public void setValueIsAdjusting(boolean valueIsAdjusting) {
		selectionModel.setValueIsAdjusting(valueIsAdjusting);
	}

	@Override
	public boolean getValueIsAdjusting() {
		return selectionModel.getValueIsAdjusting();
	}

	@Override
	public int getSelectionMode() {
		return selectionModel.getSelectionMode();
	}

	@Override
	public void addListSelectionListener(ListSelectionListener listener) {
		selectionModel.addListSelectionListener(listener);
	}

	@Override
	public void removeListSelectionListener(ListSelectionListener listener) {
		selectionModel.removeListSelectionListener(listener);
	}

	private void bindEvents() {
		singleSelectionMode.addConsumer(singleSelection ->
						setSelectionMode(singleSelection ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION));
	}

	private static void checkIndex(int index, int size) {
		if (index < 0 || index > size - 1) {
			throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
		}
	}

	private final class FilterListSelectionModel extends DefaultListSelectionModel {

		@Override
		protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
			super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
			if (!isAdjusting) {
				selectionEmpty.set(super.isSelectionEmpty());
				singleSelection.set(selectionCount() == 1);
				selectedIndex.notifyListeners();
				selectedItem.notifyListeners();
				selectedIndexes.notifyListeners();
				selectedItems.notifyListeners();
			}
		}
	}

	private final class SelectedIndex implements Observable<Integer> {

		private final Event<Integer> event = Event.event();

		@Override
		public Integer get() {
			return getMinSelectionIndex();
		}

		@Override
		public void set(Integer index) {
			requireNonNull(index);
			checkIndex(index, tableModel.visibleCount());
			setSelectionInterval(index, index);
		}

		@Override
		public EventObserver<Integer> observer() {
			return event.observer();
		}

		private void notifyListeners() {
			event.accept(get());
		}
	}

	private final class DefaultSelectedIndexes implements SelectedIndexes {

		private final Event<List<Integer>> event = Event.event();

		@Override
		public List<Integer> get() {
			if (isSelectionEmpty()) {
				return emptyList();
			}

			return unmodifiableList(IntStream.rangeClosed(getMinSelectionIndex(), getMaxSelectionIndex())
							.filter(DefaultFilterTableSelectionModel.this::isSelectedIndex)
							.boxed()
							.collect(toList()));
		}

		@Override
		public void set(Collection<Integer> indexes) {
			set(new ArrayList<>(requireNonNull(indexes)));
		}

		@Override
		public void set(List<Integer> indexes) {
			requireNonNull(indexes);
			checkIndexes(indexes);
			setValueIsAdjusting(true);
			clearSelection();
			add(indexes);
			setValueIsAdjusting(false);
		}

		@Override
		public void add(int index) {
			checkIndex(index, tableModel.visibleCount());
			addSelectionInterval(index, index);
		}

		@Override
		public void remove(int index) {
			checkIndex(index, tableModel.visibleCount());
			removeSelectionInterval(index, index);
		}

		@Override
		public void remove(Collection<Integer> indexes) {
			indexes.forEach(index -> {
				checkIndex(index, tableModel.visibleCount());
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
		public void moveUp() {
			if (tableModel.visibleCount() > 0) {
				int lastIndex = tableModel.visibleCount() - 1;
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
		public void moveDown() {
			if (tableModel.visibleCount() > 0) {
				if (isSelectionEmpty()) {
					setSelectionInterval(0, 0);
				}
				else {
					selectedIndexes.set(selectedIndexes.get().stream()
									.map(index -> index == tableModel.visibleCount() - 1 ? 0 : index + 1)
									.collect(toList()));
				}
			}
		}

		@Override
		public EventObserver<List<Integer>> observer() {
			return event.observer();
		}

		private void checkIndexes(Collection<Integer> indexes) {
			int size = tableModel.visibleCount();
			for (Integer index : indexes) {
				checkIndex(index, size);
			}
		}

		private void notifyListeners() {
			event.accept(get());
		}
	}

	private final class SelectedItem implements Observable<R> {

		private final Event<R> event = Event.event();

		@Override
		public R get() {
			int index = selectedIndex.get();
			if (index >= 0 && index < tableModel.visibleCount()) {
				return tableModel.itemAt(index);
			}

			return null;
		}

		@Override
		public void set(R item) {
			selectedItems.set(singletonList(item));
		}

		@Override
		public EventObserver<R> observer() {
			return event.observer();
		}

		private void notifyListeners() {
			event.accept(get());
		}
	}

	private final class DefaultSelectedItems implements SelectedItems<R> {

		private final Event<List<R>> event = Event.event();

		@Override
		public List<R> get() {
			return unmodifiableList(selectedIndexes.get().stream()
							.mapToInt(Integer::intValue)
							.mapToObj(tableModel::itemAt)
							.collect(toList()));
		}

		@Override
		public void set(Collection<R> items) {
			set(new ArrayList<>(requireNonNull(items)));
		}

		@Override
		public void set(List<R> items) {
			if (!isSelectionEmpty()) {
				clearSelection();
			}
			add(items);
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
			add(singletonList(item));
		}

		@Override
		public void add(Collection<R> items) {
			requireNonNull(items, "items");
			selectedIndexes.add(items.stream()
							.mapToInt(tableModel::indexOf)
							.filter(index -> index >= 0)
							.boxed()
							.collect(toList()));
		}

		@Override
		public void remove(R item) {
			remove(singletonList(requireNonNull(item)));
		}

		@Override
		public void remove(Collection<R> items) {
			requireNonNull(items).forEach(item -> selectedIndexes.remove(tableModel.indexOf(item)));
		}

		@Override
		public EventObserver<List<R>> observer() {
			return event.observer();
		}

		private List<Integer> indexesToSelect(Predicate<R> predicate) {
			List<Integer> indexes = new ArrayList<>();
			List<R> visibleItems = tableModel.visibleItems();
			for (int i = 0; i < visibleItems.size(); i++) {
				R item = visibleItems.get(i);
				if (predicate.test(item)) {
					indexes.add(i);
				}
			}

			return indexes;
		}

		private void notifyListeners() {
			event.accept(get());
		}
	}
}
