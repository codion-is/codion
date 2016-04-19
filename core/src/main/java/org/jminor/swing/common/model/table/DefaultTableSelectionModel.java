/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.table.SelectionModel;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A default table selection model implementation
 * @param <R> the type of rows
 */
public final class DefaultTableSelectionModel<R> extends DefaultListSelectionModel implements SelectionModel<R>, ListSelectionModel {

  private final Event selectionChangedEvent = Events.event();
  private final Event selectedIndexChangedEvent = Events.event();
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
   * Instantiates a new DefaultTableSelectionModel
   * @param tableModelProxy the TableModelProxy required for accessing table model items and size
   */
  public DefaultTableSelectionModel(final TableModelProxy<R> tableModelProxy) {
    this.tableModelProxy = tableModelProxy;
    this.tableModelProxy.addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(final TableModelEvent e) {
        if (e.getType() == TableModelEvent.DELETE) {
          DefaultTableSelectionModel.super.removeIndexInterval(e.getFirstRow(), e.getLastRow());
        }
      }
    });
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

    int counter = 0;
    final int min = getMinSelectionIndex();
    final int max = getMaxSelectionIndex();
    if (min >= 0 && max >= 0) {
      for (int i = min; i <= max; i++) {
        if (isSelectedIndex(i)) {
          counter++;
        }
      }
    }

    return counter;
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedIndexes(final Collection<Integer> indexes) {
    if (indexes.isEmpty()) {
      return;
    }
    checkIndexes(indexes);
    final Iterator<Integer> iterator = indexes.iterator();
    /** hold on to the first index and add last in order to avoid firing evtSelectionChanged
     *  for each index being added, see {@link #fireValueChanged(int, int, boolean)} */
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
    final List<Integer> indexes = new ArrayList<>();
    final int min = getMinSelectionIndex();
    final int max = getMaxSelectionIndex();
    for (int i = min; i <= max; i++) {
      if (isSelectedIndex(i)) {
        indexes.add(i);
      }
    }

    return indexes;
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
    else {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<R> getSelectedItems() {
    final Collection<Integer> selectedModelIndexes = getSelectedIndexes();
    final List<R> selectedItems = new ArrayList<>(selectedModelIndexes.size());
    for (final int modelIndex : selectedModelIndexes) {
      selectedItems.add(tableModelProxy.getItemAt(modelIndex));
    }

    return selectedItems;
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
    final List<Integer> indexes = new ArrayList<>();
    for (final R item : items) {
      final int index = tableModelProxy.indexOf(item);
      if (index >= 0) {
        indexes.add(index);
      }
    }
    addSelectedIndexes(indexes);
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
        final Collection<Integer> selected = getSelectedIndexes();
        final List<Integer> indexesToSelect = new ArrayList<>(selected.size());
        for (final Integer index : selected) {
          indexesToSelect.add(index == 0 ? lastIndex : index - 1);
        }
        setSelectedIndexes(indexesToSelect);
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
        final Collection<Integer> selected = getSelectedIndexes();
        final List<Integer> indexesToSelect = new ArrayList<>(selected.size());
        for (final Integer index : selected) {
          indexesToSelect.add(index == tableModelProxy.getRowCount() - 1 ? 0 : index + 1);
        }
        setSelectedIndexes(indexesToSelect);
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
      selectedIndexChangedEvent.fire();
    }
    if (!(isAdjusting || isUpdatingSelection)) {
      selectionChangedEvent.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedIndexListener(final EventListener listener) {
    selectedIndexChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectedIndexListener(final EventListener listener) {
    selectedIndexChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.addListener(listener);
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
