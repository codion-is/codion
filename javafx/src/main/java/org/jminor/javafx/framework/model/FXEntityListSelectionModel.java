/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.model.table.SelectionModel;
import org.jminor.framework.domain.Entity;

import javafx.collections.ListChangeListener;
import javafx.scene.control.MultipleSelectionModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class FXEntityListSelectionModel implements SelectionModel<Entity> {

  private final Event selectionChangedEvent = Events.event();
  private final Event<Integer> selectedIndexChangedEvent = Events.event();
  private final Event<Entity> selectedItemChangedEvent = Events.event();
  private final Event<List<Entity>> selectedItemsChangedEvent = Events.event();
  private final State selectionEmptyState = States.state(true);
  private final State multipleSelectionState = States.state(false);
  private final State singleSelectionState = States.state(false);

  private final javafx.scene.control.SelectionModel<Entity> selectionModel;

  private int selectedIndex = -1;

  /**
   * @param selectionModel the {@link javafx.scene.control.SelectionModel} instance to base this selection model on
   */
  public FXEntityListSelectionModel(final javafx.scene.control.SelectionModel<Entity> selectionModel) {
    this.selectionModel = selectionModel;
    this.selectionEmptyState.setActive(selectionModel.isEmpty());
    if (selectionModel instanceof MultipleSelectionModel) {
      this.singleSelectionState.setActive(((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().size() == 1);
      this.multipleSelectionState.setActive(!selectionEmptyState.isActive() && !singleSelectionState.isActive());
    }
    else {
      this.singleSelectionState.setActive(!selectionModel.isEmpty());
    }
    bindEvents();
  }

  /**
   * @return the underlying {@link javafx.scene.control.SelectionModel} instance
   */
  public javafx.scene.control.SelectionModel<Entity> getSelectionModel() {
    return selectionModel;
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getSelectionEmptyObserver() {
    return selectionEmptyState.getObserver();
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
  public void addSelectedIndexListener(final EventInfoListener<Integer> listener) {
    selectedIndexChangedEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectedIndexListener(final EventInfoListener listener) {
    selectedIndexChangedEvent.removeInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItemListener(final EventInfoListener<Entity> listener) {
    selectedItemChangedEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectedItemListener(final EventInfoListener listener) {
    selectedItemChangedEvent.removeInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItemsListener(final EventInfoListener<List<Entity>> listener) {
    selectedItemsChangedEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSelectedItemsListener(final EventInfoListener listener) {
    selectedItemsChangedEvent.removeInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void moveSelectionDown() {
    throw new UnsupportedOperationException("Not implemented");
  }

  /** {@inheritDoc} */
  @Override
  public void moveSelectionUp() {
    throw new UnsupportedOperationException("Not implemented");
  }

  /** {@inheritDoc} */
  @Override
  public int getSelectedIndex() {
    return selectedIndex;
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedIndex(final int index) {
    selectionModel.selectedIndexProperty().add(index);
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedIndex(final int index) {
    selectionModel.selectedIndexProperty().add(index);
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedIndexes(final Collection<Integer> indexes) {
    if (selectionModel instanceof MultipleSelectionModel) {
      selectionModel.clearSelection();
      indexes.forEach(selectionModel::select);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Integer> getSelectedIndexes() {
    if (selectionModel instanceof MultipleSelectionModel) {
      return Collections.unmodifiableList(new ArrayList<>(((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices()));
    }
    else if (selectionModel.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.singletonList(selectionModel.selectedIndexProperty().get());
  }

  /** {@inheritDoc} */
  @Override
  public void selectAll() {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).selectAll();
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedIndexes(final Collection<Integer> indexes) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().addAll(indexes);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getSelectionCount() {
    if (selectionModel instanceof MultipleSelectionModel) {
      return ((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().size();
    }

    return selectionModel.getSelectedIndex() == -1 ? 0 : 1;
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItems(final Collection<Entity> items) {
    if (selectionModel instanceof MultipleSelectionModel) {
      selectionModel.clearSelection();
      items.forEach(selectionModel::select);
    }
    else if (items.isEmpty()) {
      selectionModel.clearSelection();
    }
    else {
      selectionModel.select(items.iterator().next());
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> getSelectedItems() {
    if (selectionModel instanceof MultipleSelectionModel) {
      return Collections.unmodifiableList(new ArrayList<>(((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems()));
    }
    else if (selectionModel.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.singletonList(selectionModel.getSelectedItem());
  }

  /** {@inheritDoc} */
  @Override
  public Entity getSelectedItem() {
    if (selectionModel.isEmpty()) {
      return null;
    }
    else if (selectionModel instanceof MultipleSelectionModel) {
      return ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().get(0);
    }

    return selectionModel.getSelectedItem();
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItem(final Entity item) {
    if (selectionModel instanceof MultipleSelectionModel) {
      selectionModel.clearSelection();
      selectionModel.select(item);
    }
    else {
      selectionModel.select(item);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItem(final Entity item) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().add(item);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItems(final Collection<Entity> items) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().addAll(items);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void clearSelection() {
    selectionModel.clearSelection();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSelectionEmpty() {
    return selectionModel.isEmpty();
  }

  private void bindEvents() {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().addListener((ListChangeListener<Entity>) change -> {
        selectionEmptyState.setActive(selectionModel.isEmpty());
        final List<Integer> selectedIndices = getSelectedIndexes();
        singleSelectionState.setActive(selectedIndices.size() == 1);
        multipleSelectionState.setActive(!selectionEmptyState.isActive() && !singleSelectionState.isActive());
        selectionChangedEvent.fire();
        selectedItemChangedEvent.fire(getSelectedItem());
        selectedItemsChangedEvent.fire(getSelectedItems());
      });
      selectionModel.selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
        final int newSelectedIndex = newValue.intValue();
        if (selectedIndex != newSelectedIndex) {
          selectedIndex = newSelectedIndex;
          selectedIndexChangedEvent.fire(selectedIndex);
        }
      });
    }
    else {
      selectionModel.selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
        final int newSelectedIndex = newValue.intValue();
        if (selectedIndex != newSelectedIndex) {
          selectedIndex = newSelectedIndex;
          selectedIndexChangedEvent.fire(selectedIndex);
          selectedItemChangedEvent.fire(getSelectedItem());
          selectedItemsChangedEvent.fire(getSelectedItems());
        }
      });
    }
  }
}
