/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.Event;
import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.model.table.SelectionModel;
import org.jminor.common.model.table.TableModelProxy;

import javax.swing.DefaultListSelectionModel;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A default table selection model implementation
 * @param <R> the type of rows
 */
public final class SwingTableSelectionModel<R> extends DefaultListSelectionModel implements SelectionModel<R> {

  private final Event selectionChangedEvent = Events.event();
  private final Event<Integer> selectedIndexChangedEvent = Events.event();
  private final Event<R> selectedItemChangedEvent = Events.event();
  private final Event<List<R>> selectedItemsChangedEvent = Events.event();
  private final State selectionEmptyState = States.state(true);
  private final State multipleSelectionState = States.state(false);
  private final State singleSelectionState = States.state(false);

  /**
   * true while the selection is being updated
   */
  private boolean isUpdatingSelection = false;

  /**
   * Holds the topmost (minimum) selected index
   */
  private int selectedIndex = -1;

  /**
   * The TableModel proxy
   */
  private final TableModelProxy<R> tableModelProxy;

  /**
   * Instantiates a new SwingTableSelectionModel
   * @param tableModelProxy the TableModelProxy required for accessing table model items and size
   */
  public SwingTableSelectionModel(final TableModelProxy<R> tableModelProxy) {
    this.tableModelProxy = tableModelProxy;
    this.tableModelProxy.addRowsDeletedListener(interval ->
            SwingTableSelectionModel.super.removeIndexInterval(interval.get(0), interval.get(1)));
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedIndex(final int index) {
    checkIndex(index, tableModelProxy.getRowCount());
    addSelectionInterval(index, index);
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedIndex(final int index) {
    checkIndex(index, tableModelProxy.getRowCount());
    setSelectionInterval(index, index);
  }

  /** {@inheritDoc} */
  @Override
  public int getSelectionCount() {
    if (isSelectionEmpty()) {
      return 0;
    }

    return (int) IntStream.rangeClosed(getMinSelectionIndex(), getMaxSelectionIndex())
            .filter(this::isSelectedIndex).count();
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedIndexes(final Collection<Integer> indexes) {
    if (indexes.isEmpty()) {
      return;
    }
    checkIndexes(indexes);
    final Iterator<Integer> iterator = indexes.iterator();
    /* hold on to the first index and add last in order to avoid firing selectionChanged
     * for each index being added, see fireValueChanged(int, int, boolean) */
    final int firstIndex = iterator.next();
    try {
      isUpdatingSelection = true;
      while (iterator.hasNext()) {
        final int index = iterator.next();
        addSelectionInterval(index, index);
      }
    }
    finally {
      isUpdatingSelection = false;
      addSelectionInterval(firstIndex, firstIndex);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedIndexes(final Collection<Integer> indexes) {
    checkIndexes(indexes);
    clearSelection();
    addSelectedIndexes(indexes);
  }

  /** {@inheritDoc} */
  @Override
  public List<Integer> getSelectedIndexes() {
    if (isSelectionEmpty()) {
      return Collections.emptyList();
    }

    return IntStream.rangeClosed(getMinSelectionIndex(), getMaxSelectionIndex())
            .filter(this::isSelectedIndex).boxed().collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public int getSelectedIndex() {
    return selectedIndex;
  }

  /** {@inheritDoc} */
  @Override
  public void selectAll() {
    setSelectionInterval(0, tableModelProxy.getRowCount() - 1);
  }

  /** {@inheritDoc} */
  @Override
  public R getSelectedItem() {
    final int index = getSelectedIndex();
    if (index >= 0 && index < tableModelProxy.getRowCount()) {
      return tableModelProxy.getItemAt(index);
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public List<R> getSelectedItems() {
    final Collection<Integer> selectedModelIndexes = getSelectedIndexes();

    return selectedModelIndexes.stream().mapToInt(modelIndex ->
            modelIndex).mapToObj(tableModelProxy::getItemAt).collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItem(final R item) {
    setSelectedItems(Collections.singletonList(item));
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItems(final Collection<R> items) {
    if (!isSelectionEmpty()) {
      clearSelection();
    }
    addSelectedItems(items);
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItem(final R item) {
    addSelectedItems(Collections.singletonList(item));
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItems(final Collection<R> items) {
    addSelectedIndexes(items.stream().mapToInt(tableModelProxy::indexOf)
            .filter(index -> index >= 0).boxed().collect(Collectors.toList()));
  }

  @Override
  public void addSelectionInterval(final int fromIndex, final int toIndex) {
    if (tableModelProxy.allowSelectionChange()) {
      super.addSelectionInterval(fromIndex, toIndex);
    }
  }

  @Override
  public void setSelectionInterval(final int fromIndex, final int toIndex) {
    if (tableModelProxy.allowSelectionChange()) {
      super.setSelectionInterval(fromIndex, toIndex);
    }
  }

  @Override
  public void removeSelectionInterval(final int fromIndex, final int toIndex) {
    if (tableModelProxy.allowSelectionChange()) {
      super.removeSelectionInterval(fromIndex, toIndex);
    }
  }

  @Override
  public void insertIndexInterval(final int fromIndex, final int length, final boolean before) {
    if (tableModelProxy.allowSelectionChange()) {
      super.insertIndexInterval(fromIndex, length, before);
    }
  }

  @Override
  public void removeIndexInterval(final int fromIndex, final int toIndex) {
    if (tableModelProxy.allowSelectionChange()) {
      super.removeIndexInterval(fromIndex, toIndex);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void moveSelectionUp() {
    if (tableModelProxy.getRowCount() > 0) {
      final int lastIndex = tableModelProxy.getRowCount() - 1;
      if (isSelectionEmpty()) {
        setSelectionInterval(lastIndex, lastIndex);
      }
      else {
        setSelectedIndexes(getSelectedIndexes().stream()
                .map(index -> index == 0 ? lastIndex : index - 1).collect(Collectors.toList()));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void moveSelectionDown() {
    if (tableModelProxy.getRowCount() > 0) {
      if (isSelectionEmpty()) {
        setSelectionInterval(0, 0);
      }
      else {
        setSelectedIndexes(getSelectedIndexes().stream()
                .map(index -> index == tableModelProxy.getRowCount() - 1 ? 0 : index + 1).collect(Collectors.toList()));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void fireValueChanged(final int firstIndex, final int lastIndex, final boolean isAdjusting) {
    super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
    selectionEmptyState.setActive(isSelectionEmpty());
    singleSelectionState.setActive(getSelectionCount() == 1);
    multipleSelectionState.setActive(!selectionEmptyState.isActive() && !singleSelectionState.isActive());
    final int minSelIndex = getMinSelectionIndex();
    if (selectedIndex != minSelIndex) {
      selectedIndex = minSelIndex;
      selectedIndexChangedEvent.fire(selectedIndex);
      selectedItemChangedEvent.fire(getSelectedItem());
    }
    if (!(isAdjusting || isUpdatingSelection)) {
      selectionChangedEvent.fire();
      selectedItemsChangedEvent.fire(getSelectedItems());
    }
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedIndexListener(final EventDataListener<Integer> listener) {
    selectedIndexChangedEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectedIndexListener(final EventDataListener listener) {
    selectedIndexChangedEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItemListener(final EventDataListener<R> listener) {
    selectedItemChangedEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectedItemListener(final EventDataListener listener) {
    selectedItemChangedEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItemsListener(final EventDataListener<List<R>> listener) {
    selectedItemsChangedEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectedItemsListener(final EventDataListener listener) {
    selectedItemsChangedEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getMultipleSelectionObserver() {
    return multipleSelectionState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getSingleSelectionObserver() {
    return singleSelectionState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getSelectionEmptyObserver() {
    return selectionEmptyState.getObserver();
  }

  private void checkIndexes(final Collection<Integer> indexes) {
    final int size = tableModelProxy.getRowCount();
    for (final Integer index : indexes) {
      checkIndex(index, size);
    }
  }

  private void checkIndex(final int index, final int size) {
    if (index < 0 || index > size - 1) {
      throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
    }
  }
}
