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
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import javax.swing.DefaultListSelectionModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultFilterTableSelectionModel<R> extends DefaultListSelectionModel implements FilterTableSelectionModel<R> {

	private final Event<?> selectionChangingEvent = Event.event();
	private final Event<?> selectionEvent = Event.event();
	private final Event<Integer> selectedIndexEvent = Event.event();
	private final Event<List<Integer>> selectedIndexesEvent = Event.event();
	private final Event<R> selectedItemEvent = Event.event();
	private final Event<List<R>> selectedItemsEvent = Event.event();
	private final State singleSelectionMode = State.state(false);
	private final State selectionEmpty = State.state(true);
	private final State singleSelection = State.state(false);
	private final StateObserver multipleSelection = State.and(selectionEmpty.not(), singleSelection.not());

	/**
	 * Holds the topmost (minimum) selected index
	 */
	private int selectedIndex = -1;

	/**
	 * The table model
	 */
	private final FilterTableModel<R, ?> tableModel;

	DefaultFilterTableSelectionModel(FilterTableModel<R, ?> tableModel) {
		this.tableModel = requireNonNull(tableModel, "tableModel");
		bindEvents();
	}

	@Override
	public void setSelectionMode(int selectionMode) {
		if (getSelectionMode() != selectionMode) {
			clearSelection();
			super.setSelectionMode(selectionMode);
			singleSelectionMode.set(selectionMode == SINGLE_SELECTION);
		}
	}

	@Override
	public State singleSelectionMode() {
		return singleSelectionMode;
	}

	@Override
	public void addSelectedIndex(int index) {
		checkIndex(index, tableModel.visibleCount());
		addSelectionInterval(index, index);
	}

	@Override
	public void removeSelectedIndex(int index) {
		checkIndex(index, tableModel.visibleCount());
		removeSelectionInterval(index, index);
	}

	@Override
	public void removeSelectedIndexes(Collection<Integer> indexes) {
		indexes.forEach(index -> {
			checkIndex(index, tableModel.visibleCount());
			removeSelectionInterval(index, index);
		});
	}

	@Override
	public void setSelectedIndex(int index) {
		checkIndex(index, tableModel.visibleCount());
		setSelectionInterval(index, index);
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
	public void addSelectedIndexes(Collection<Integer> indexes) {
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
	public void setSelectedIndexes(Collection<Integer> indexes) {
		requireNonNull(indexes);
		checkIndexes(indexes);
		setValueIsAdjusting(true);
		clearSelection();
		addSelectedIndexes(indexes);
		setValueIsAdjusting(false);
	}

	@Override
	public List<Integer> getSelectedIndexes() {
		if (isSelectionEmpty()) {
			return emptyList();
		}

		return IntStream.rangeClosed(getMinSelectionIndex(), getMaxSelectionIndex())
						.filter(this::isSelectedIndex)
						.boxed()
						.collect(toList());
	}

	@Override
	public int getSelectedIndex() {
		return selectedIndex;
	}

	@Override
	public void selectAll() {
		setSelectionInterval(0, tableModel.visibleCount() - 1);
	}

	@Override
	public void setSelectedItems(Predicate<R> predicate) {
		setSelectedIndexes(indexesToSelect(requireNonNull(predicate)));
	}

	@Override
	public void addSelectedItems(Predicate<R> predicate) {
		addSelectedIndexes(indexesToSelect(requireNonNull(predicate)));
	}

	@Override
	public R getSelectedItem() {
		int index = getSelectedIndex();
		if (index >= 0 && index < tableModel.visibleCount()) {
			return tableModel.itemAt(index);
		}

		return null;
	}

	@Override
	public Optional<R> selectedItem() {
		return Optional.ofNullable(getSelectedItem());
	}

	@Override
	public boolean isSelected(R item) {
		requireNonNull(item);

		return isSelectedIndex(tableModel.indexOf(item));
	}

	@Override
	public List<R> getSelectedItems() {
		return getSelectedIndexes().stream()
						.mapToInt(Integer::intValue)
						.mapToObj(tableModel::itemAt)
						.collect(toList());
	}

	@Override
	public void setSelectedItem(R item) {
		setSelectedItems(singletonList(item));
	}

	@Override
	public void setSelectedItems(Collection<R> items) {
		if (!isSelectionEmpty()) {
			clearSelection();
		}
		addSelectedItems(items);
	}

	@Override
	public void addSelectedItem(R item) {
		addSelectedItems(singletonList(item));
	}

	@Override
	public void addSelectedItems(Collection<R> items) {
		requireNonNull(items, "items");
		addSelectedIndexes(items.stream()
						.mapToInt(tableModel::indexOf)
						.filter(index -> index >= 0)
						.boxed()
						.collect(toList()));
	}

	@Override
	public void removeSelectedItem(R item) {
		removeSelectedItems(singletonList(requireNonNull(item)));
	}

	@Override
	public void removeSelectedItems(Collection<R> items) {
		requireNonNull(items).forEach(item -> removeSelectedIndex(tableModel.indexOf(item)));
	}

	@Override
	public void addSelectionInterval(int fromIndex, int toIndex) {
		selectionChangingEvent.run();
		super.addSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void setSelectionInterval(int fromIndex, int toIndex) {
		selectionChangingEvent.run();
		super.setSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void removeSelectionInterval(int fromIndex, int toIndex) {
		selectionChangingEvent.run();
		super.removeSelectionInterval(fromIndex, toIndex);
	}

	@Override
	public void insertIndexInterval(int fromIndex, int length, boolean before) {
		selectionChangingEvent.run();
		super.insertIndexInterval(fromIndex, length, before);
	}

	@Override
	public void removeIndexInterval(int fromIndex, int toIndex) {
		selectionChangingEvent.run();
		super.removeIndexInterval(fromIndex, toIndex);
	}

	@Override
	public void moveSelectionUp() {
		if (tableModel.visibleCount() > 0) {
			int lastIndex = tableModel.visibleCount() - 1;
			if (isSelectionEmpty()) {
				setSelectionInterval(lastIndex, lastIndex);
			}
			else {
				setSelectedIndexes(getSelectedIndexes().stream()
								.map(index -> index == 0 ? lastIndex : index - 1)
								.collect(toList()));
			}
		}
	}

	@Override
	public void moveSelectionDown() {
		if (tableModel.visibleCount() > 0) {
			if (isSelectionEmpty()) {
				setSelectionInterval(0, 0);
			}
			else {
				setSelectedIndexes(getSelectedIndexes().stream()
								.map(index -> index == tableModel.visibleCount() - 1 ? 0 : index + 1)
								.collect(toList()));
			}
		}
	}

	@Override
	public void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
		super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
		if (!isAdjusting) {
			selectionEmpty.set(isSelectionEmpty());
			singleSelection.set(selectionCount() == 1);
			int minSelIndex = getMinSelectionIndex();
			if (selectedIndex != minSelIndex) {
				selectedIndex = minSelIndex;
				selectedIndexEvent.accept(selectedIndex);
				selectedItemEvent.accept(getSelectedItem());
			}
			List<Integer> selectedIndexes = getSelectedIndexes();
			selectionEvent.run();
			selectedIndexesEvent.accept(selectedIndexes);
			//we don't call getSelectedItems() since that would cause another call to getSelectedIndexes()
			selectedItemsEvent.accept(selectedIndexes.stream()
							.mapToInt(modelIndex -> modelIndex)
							.mapToObj(tableModel::itemAt)
							.collect(toList()));
		}
	}

	@Override
	public EventObserver<?> selectionChangingEvent() {
		return selectionChangingEvent.observer();
	}

	@Override
	public EventObserver<Integer> selectedIndexEvent() {
		return selectedIndexEvent.observer();
	}

	@Override
	public EventObserver<List<Integer>> selectedIndexesEvent() {
		return selectedIndexesEvent.observer();
	}

	@Override
	public EventObserver<?> selectionEvent() {
		return selectionEvent.observer();
	}

	@Override
	public EventObserver<R> selectedItemEvent() {
		return selectedItemEvent.observer();
	}

	@Override
	public EventObserver<List<R>> selectedItemsEvent() {
		return selectedItemsEvent.observer();
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

	private void bindEvents() {
		singleSelectionMode.addConsumer(singleSelection ->
						setSelectionMode(singleSelection ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION));
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

	private void checkIndexes(Collection<Integer> indexes) {
		int size = tableModel.visibleCount();
		for (Integer index : indexes) {
			checkIndex(index, size);
		}
	}

	private static void checkIndex(int index, int size) {
		if (index < 0 || index > size - 1) {
			throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
		}
	}
}
