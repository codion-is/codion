/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.SelectionModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import javax.swing.DefaultListSelectionModel;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default table selection model implementation
 * @param <R> the type of rows
 */
public final class SwingTableSelectionModel<R> extends DefaultListSelectionModel implements SelectionModel<R> {

  private final Event<?> selectionChangedEvent = Event.event();
  private final Event<Integer> selectedIndexChangedEvent = Event.event();
  private final Event<List<Integer>> selectedIndexesChangedEvent = Event.event();
  private final Event<R> selectedItemChangedEvent = Event.event();
  private final Event<List<R>> selectedItemsChangedEvent = Event.event();
  private final State singleSelectionModeState = State.state(false);
  private final State selectionEmptyState = State.state(true);
  private final State multipleSelectionState = State.state(false);
  private final State singleSelectionState = State.state(false);

  /**
   * Holds the topmost (minimum) selected index
   */
  private int selectedIndex = -1;

  /**
   * The table model
   */
  private final FilteredTableModel<R, ?> tableModel;

  /**
   * Instantiates a new SwingTableSelectionModel
   * @param tableModel the FilteredTableModel required for accessing table model items and size
   */
  public SwingTableSelectionModel(final FilteredTableModel<R, ?> tableModel) {
    this.tableModel = requireNonNull(tableModel, "tableModel");
    this.tableModel.addRowsRemovedListener(removal ->
            SwingTableSelectionModel.super.removeIndexInterval(removal.getFromRow(), removal.getToRow()));
    bindEvents();
  }

  @Override
  public void setSelectionMode(final int selectionMode) {
    if (getSelectionMode() != selectionMode) {
      clearSelection();
      super.setSelectionMode(selectionMode);
      singleSelectionModeState.set(selectionMode == SINGLE_SELECTION);
    }
  }

  @Override
  public State getSingleSelectionModeState() {
    return singleSelectionModeState;
  }

  @Override
  public void addSelectedIndex(final int index) {
    checkIndex(index, tableModel.getRowCount());
    addSelectionInterval(index, index);
  }

  @Override
  public void removeSelectedIndex(final int index) {
    checkIndex(index, tableModel.getRowCount());
    removeSelectionInterval(index, index);
  }

  @Override
  public void removeSelectedIndexes(final Collection<Integer> indexes) {
    indexes.forEach(index -> {
      checkIndex(index, tableModel.getRowCount());
      removeSelectionInterval(index, index);
    });
  }

  @Override
  public void setSelectedIndex(final int index) {
    checkIndex(index, tableModel.getRowCount());
    setSelectionInterval(index, index);
  }

  @Override
  public int getSelectionCount() {
    if (isSelectionEmpty()) {
      return 0;
    }

    return (int) IntStream.rangeClosed(getMinSelectionIndex(), getMaxSelectionIndex())
            .filter(this::isSelectedIndex).count();
  }

  @Override
  public void addSelectedIndexes(final Collection<Integer> indexes) {
    if (requireNonNull(indexes).isEmpty()) {
      return;
    }
    checkIndexes(indexes);
    setValueIsAdjusting(true);
    for (final Integer index : indexes) {
      addSelectionInterval(index, index);
    }
    setValueIsAdjusting(false);
  }

  @Override
  public void setSelectedIndexes(final Collection<Integer> indexes) {
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
            .filter(this::isSelectedIndex).boxed().collect(toList());
  }

  @Override
  public int getSelectedIndex() {
    return selectedIndex;
  }

  @Override
  public void selectAll() {
    setSelectionInterval(0, tableModel.getRowCount() - 1);
  }

  @Override
  public R getSelectedItem() {
    int index = getSelectedIndex();
    if (index >= 0 && index < tableModel.getRowCount()) {
      return tableModel.getItemAt(index);
    }

    return null;
  }

  @Override
  public List<R> getSelectedItems() {
    return getSelectedIndexes().stream()
            .mapToInt(modelIndex ->modelIndex).mapToObj(tableModel::getItemAt)
            .collect(toList());
  }

  @Override
  public void setSelectedItem(final R item) {
    setSelectedItems(singletonList(item));
  }

  @Override
  public void setSelectedItems(final Collection<R> items) {
    if (!isSelectionEmpty()) {
      clearSelection();
    }
    addSelectedItems(items);
  }

  @Override
  public void addSelectedItem(final R item) {
    addSelectedItems(singletonList(item));
  }

  @Override
  public void addSelectedItems(final Collection<R> items) {
    requireNonNull(items, "items");
    addSelectedIndexes(items.stream()
            .mapToInt(tableModel::indexOf)
            .filter(index -> index >= 0)
            .boxed()
            .collect(toList()));
  }

  @Override
  public void removeSelectedItem(final R item) {
    removeSelectedItems(singletonList(requireNonNull(item)));
  }

  @Override
  public void removeSelectedItems(final Collection<R> items) {
    requireNonNull(items).forEach(item -> removeSelectedIndex(tableModel.indexOf(item)));
  }

  @Override
  public void addSelectionInterval(final int fromIndex, final int toIndex) {
    if (tableModel.allowSelectionChange()) {
      super.addSelectionInterval(fromIndex, toIndex);
    }
  }

  @Override
  public void setSelectionInterval(final int fromIndex, final int toIndex) {
    if (tableModel.allowSelectionChange()) {
      super.setSelectionInterval(fromIndex, toIndex);
    }
  }

  @Override
  public void removeSelectionInterval(final int fromIndex, final int toIndex) {
    if (tableModel.allowSelectionChange()) {
      super.removeSelectionInterval(fromIndex, toIndex);
    }
  }

  @Override
  public void insertIndexInterval(final int fromIndex, final int length, final boolean before) {
    if (tableModel.allowSelectionChange()) {
      super.insertIndexInterval(fromIndex, length, before);
    }
  }

  @Override
  public void removeIndexInterval(final int fromIndex, final int toIndex) {
    if (tableModel.allowSelectionChange()) {
      super.removeIndexInterval(fromIndex, toIndex);
    }
  }

  @Override
  public void moveSelectionUp() {
    if (tableModel.getRowCount() > 0) {
      int lastIndex = tableModel.getRowCount() - 1;
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
    if (tableModel.getRowCount() > 0) {
      if (isSelectionEmpty()) {
        setSelectionInterval(0, 0);
      }
      else {
        setSelectedIndexes(getSelectedIndexes().stream()
                .map(index -> index == tableModel.getRowCount() - 1 ? 0 : index + 1)
                .collect(toList()));
      }
    }
  }

  @Override
  public boolean isSelectionNotEmpty() {
    return !isSelectionEmpty();
  }

  @Override
  public void fireValueChanged(final int firstIndex, final int lastIndex, final boolean isAdjusting) {
    super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
    if (!isAdjusting) {
      selectionEmptyState.set(isSelectionEmpty());
      singleSelectionState.set(getSelectionCount() == 1);
      multipleSelectionState.set(!selectionEmptyState.get() && !singleSelectionState.get());
      int minSelIndex = getMinSelectionIndex();
      if (selectedIndex != minSelIndex) {
        selectedIndex = minSelIndex;
        selectedIndexChangedEvent.onEvent(selectedIndex);
        selectedItemChangedEvent.onEvent(getSelectedItem());
      }
      List<Integer> selectedIndexes = getSelectedIndexes();
      selectionChangedEvent.onEvent();
      selectedIndexesChangedEvent.onEvent(selectedIndexes);
      //we don't call getSelectedItems() since that would cause another call to getSelectedIndexes()
      selectedItemsChangedEvent.onEvent(selectedIndexes.stream()
              .mapToInt(modelIndex -> modelIndex)
              .mapToObj(tableModel::getItemAt)
              .collect(toList()));
    }
  }

  @Override
  public void addSelectedIndexListener(final EventDataListener<Integer> listener) {
    selectedIndexChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeSelectedIndexListener(final EventDataListener<Integer> listener) {
    selectedIndexChangedEvent.removeDataListener(listener);
  }

  @Override
  public void addSelectedIndexesListener(final EventDataListener<List<Integer>> listener) {
    selectedIndexesChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeSelectedIndexesListener(final EventDataListener<List<Integer>> listener) {
    selectedIndexesChangedEvent.removeDataListener(listener);
  }

  @Override
  public void addSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.addListener(listener);
  }

  @Override
  public void removeSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.removeListener(listener);
  }

  @Override
  public void addSelectedItemListener(final EventDataListener<R> listener) {
    selectedItemChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeSelectedItemListener(final EventDataListener<R> listener) {
    selectedItemChangedEvent.removeDataListener(listener);
  }

  @Override
  public void addSelectedItemsListener(final EventDataListener<List<R>> listener) {
    selectedItemsChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeSelectedItemsListener(final EventDataListener<List<R>> listener) {
    selectedItemsChangedEvent.removeDataListener(listener);
  }

  @Override
  public StateObserver getMultipleSelectionObserver() {
    return multipleSelectionState.getObserver();
  }

  @Override
  public StateObserver getSingleSelectionObserver() {
    return singleSelectionState.getObserver();
  }

  @Override
  public StateObserver getSelectionEmptyObserver() {
    return selectionEmptyState.getObserver();
  }

  @Override
  public StateObserver getSelectionNotEmptyObserver() {
    return selectionEmptyState.getReversedObserver();
  }

  private void bindEvents() {
    singleSelectionModeState.addDataListener(singleSelection ->
            setSelectionMode(singleSelection ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION));
  }

  private void checkIndexes(final Collection<Integer> indexes) {
    int size = tableModel.getRowCount();
    for (final Integer index : indexes) {
      checkIndex(index, size);
    }
  }

  private static void checkIndex(final int index, final int size) {
    if (index < 0 || index > size - 1) {
      throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
    }
  }
}
