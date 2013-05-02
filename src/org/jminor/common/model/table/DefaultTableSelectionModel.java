/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;

import javax.swing.DefaultListSelectionModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A default table selection model implementation
 * @param <R> the type of rows
 */
public final class DefaultTableSelectionModel<R> extends DefaultListSelectionModel implements TableSelectionModel<R> {

  private final Event evtSelectionChanged = Events.event();
  private final Event evtSelectedIndexChanged = Events.event();
  private final State stSelectionEmpty = States.state(true);
  private final State stMultipleSelection = States.state(false);
  private final State stSingleSelection = States.state(false);

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
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedIndex(final int index) {
    checkIndex(index, tableModelProxy.getSize());
    addSelectionInterval(index, index);
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedIndex(final int index) {
    checkIndex(index, tableModelProxy.getSize());
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
    //keep the first index and add last in order to avoid firing evtSelectionChanged
    //for each index being added, see fireValueChanged() above
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
    final List<Integer> indexes = new ArrayList<Integer>();
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
    setSelectionInterval(0, tableModelProxy.getSize() - 1);
  }

  /** {@inheritDoc} */
  @Override
  public R getSelectedItem() {
    final int index = getSelectedIndex();
    if (index >= 0 && index < tableModelProxy.getSize()) {
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
    final List<R> selectedItems = new ArrayList<R>(selectedModelIndexes.size());
    for (final int modelIndex : selectedModelIndexes) {
      selectedItems.add(tableModelProxy.getItemAt(modelIndex));
    }

    return selectedItems;
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItem(final R item) {
    setSelectedItems(Arrays.asList(item));
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
    addSelectedItems(Arrays.asList(item));
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItems(final Collection<R> items) {
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final R item : items) {
      final int index = tableModelProxy.indexOf(item);
      if (index >= 0) {
        indexes.add(index);
      }
    }
    addSelectedIndexes(indexes);
  }

  /** {@inheritDoc} */
  @Override
  public void moveSelectionUp() {
    if (tableModelProxy.getSize() > 0) {
      final int lastIndex = tableModelProxy.getSize() - 1;
      if (isSelectionEmpty()) {
        setSelectionInterval(lastIndex, lastIndex);
      }
      else {
        final Collection<Integer> selected = getSelectedIndexes();
        final List<Integer> indexesToSelect = new ArrayList<Integer>(selected.size());
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
    if (tableModelProxy.getSize() > 0) {
      if (isSelectionEmpty()) {
        setSelectionInterval(0, 0);
      }
      else {
        final Collection<Integer> selected = getSelectedIndexes();
        final List<Integer> indexesToSelect = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected) {
          indexesToSelect.add(index == tableModelProxy.getSize() - 1 ? 0 : index + 1);
        }
        setSelectedIndexes(indexesToSelect);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void fireValueChanged(final int firstIndex, final int lastIndex, final boolean isAdjusting) {
    super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
    stSelectionEmpty.setActive(isSelectionEmpty());
    stSingleSelection.setActive(getSelectionCount() == 1);
    stMultipleSelection.setActive(!stSelectionEmpty.isActive() && !stSingleSelection.isActive());
    final int minSelIndex = getMinSelectionIndex();
    if (selectedIndex != minSelIndex) {
      selectedIndex = minSelIndex;
      evtSelectedIndexChanged.fire();
    }
    if (!(isAdjusting || isUpdatingSelection)) {
      evtSelectionChanged.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedIndexListener(final EventListener listener) {
    evtSelectedIndexChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectedIndexListener(final EventListener listener) {
    evtSelectedIndexChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectionChangedListener(final EventListener listener) {
    evtSelectionChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectionChangedListener(final EventListener listener) {
    evtSelectionChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getMultipleSelectionObserver() {
    return stMultipleSelection.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getSingleSelectionObserver() {
    return stSingleSelection.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getSelectionEmptyObserver() {
    return stSelectionEmpty.getObserver();
  }

  private void checkIndexes(final Collection<Integer> indexes) {
    final int size = tableModelProxy.getSize();
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
