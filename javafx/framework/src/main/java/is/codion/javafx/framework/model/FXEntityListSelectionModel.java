/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.TableSelectionModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Entity;

import javafx.collections.ListChangeListener;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A JavaFX {@link TableSelectionModel} implementation
 */
public final class FXEntityListSelectionModel implements TableSelectionModel<Entity> {

  private final Event<?> selectionEvent = Event.event();
  private final Event<Integer> selectedIndexEvent = Event.event();
  private final Event<List<Integer>> selectedIndexesEvent = Event.event();
  private final Event<Entity> selectedItemEvent = Event.event();
  private final Event<List<Entity>> selectedItemsEvent = Event.event();
  private final State singleSelectionModeState = State.state(false);
  private final State selectionEmptyState = State.state(true);
  private final State multipleSelectionState = State.state(false);
  private final State singleSelectionState = State.state(false);

  private final FilteredModel<Entity> filteredModel;
  private final javafx.scene.control.SelectionModel<Entity> selectionModel;

  private int selectedIndex = -1;

  /**
   * @param filteredModel the filtered model
   * @param selectionModel the {@link javafx.scene.control.SelectionModel} instance to base this selection model on
   */
  public FXEntityListSelectionModel(FilteredModel<Entity> filteredModel, javafx.scene.control.SelectionModel<Entity> selectionModel) {
    this.filteredModel = requireNonNull(filteredModel);
    this.selectionModel = requireNonNull(selectionModel);
    this.selectionEmptyState.set(selectionModel.isEmpty());
    if (selectionModel instanceof MultipleSelectionModel) {
      this.singleSelectionState.set(((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().size() == 1);
      this.multipleSelectionState.set(!selectionEmptyState.get() && !singleSelectionState.get());
    }
    else {
      this.singleSelectionState.set(!selectionModel.isEmpty());
    }
    bindEvents();
  }

  /**
   * @return the underlying {@link javafx.scene.control.SelectionModel} instance
   */
  public javafx.scene.control.SelectionModel<Entity> selectionModel() {
    return selectionModel;
  }

  @Override
  public State singleSelectionModeState() {
    return singleSelectionModeState;
  }

  @Override
  public StateObserver selectionEmptyObserver() {
    return selectionEmptyState.observer();
  }

  @Override
  public StateObserver selectionNotEmptyObserver() {
    return selectionEmptyState.reversedObserver();
  }

  @Override
  public StateObserver multipleSelectionObserver() {
    return multipleSelectionState.observer();
  }

  @Override
  public StateObserver singleSelectionObserver() {
    return singleSelectionState.observer();
  }

  @Override
  public void addBeforeSelectionChangeListener(EventListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeBeforeSelectionChangeListener(EventListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addSelectionListener(EventListener listener) {
    selectionEvent.addListener(listener);
  }

  @Override
  public void removeSelectionListener(EventListener listener) {
    selectionEvent.removeListener(listener);
  }

  @Override
  public void addSelectedIndexListener(EventDataListener<Integer> listener) {
    selectedIndexEvent.addDataListener(listener);
  }

  @Override
  public void removeSelectedIndexListener(EventDataListener<Integer> listener) {
    selectedIndexEvent.removeDataListener(listener);
  }

  @Override
  public void addSelectedIndexesListener(EventDataListener<List<Integer>> listener) {
    selectedIndexesEvent.addDataListener(listener);
  }

  @Override
  public void removeSelectedIndexesListener(EventDataListener<List<Integer>> listener) {
    selectedIndexesEvent.removeDataListener(listener);
  }

  @Override
  public void addSelectedItemListener(EventDataListener<Entity> listener) {
    selectedItemEvent.addDataListener(listener);
  }

  @Override
  public void removeSelectedItemListener(EventDataListener<Entity> listener) {
    selectedItemEvent.removeDataListener(listener);
  }

  @Override
  public void addSelectedItemsListener(EventDataListener<List<Entity>> listener) {
    selectedItemsEvent.addDataListener(listener);
  }

  @Override
  public void removeSelectedItemsListener(EventDataListener<List<Entity>> listener) {
    selectedItemsEvent.removeDataListener(listener);
  }

  @Override
  public void moveSelectionDown() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void moveSelectionUp() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getSelectedIndex() {
    return selectedIndex;
  }

  @Override
  public void addSelectedIndex(int index) {
    selectionModel.selectedIndexProperty().add(index);
  }

  @Override
  public void removeSelectedIndex(int index) {
    if (selectionModel instanceof MultipleSelectionModel) {
      removeSelectedIndexes(singletonList(index));
    }
    else {
      if (selectionModel.selectedIndexProperty().get() == index) {
        selectionModel.clearSelection();
      }
    }
  }

  @Override
  public void removeSelectedIndexes(Collection<Integer> indexes) {
    if (selectionModel instanceof MultipleSelectionModel) {
      indexes.forEach(index -> ((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().remove(index));
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void removeSelectedItem(Entity item) {
    removeSelectedItems(singletonList(item));
  }

  @Override
  public void removeSelectedItems(Collection<Entity> items) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().removeAll(items);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void setSelectedIndex(int index) {
    selectionModel.clearSelection();
    selectionModel.select(index);
  }

  @Override
  public void setSelectedIndexes(Collection<Integer> indexes) {
    if (selectionModel instanceof MultipleSelectionModel) {
      selectionModel.clearSelection();
      indexes.forEach(selectionModel::select);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public List<Integer> getSelectedIndexes() {
    if (selectionModel instanceof MultipleSelectionModel) {
      return Collections.unmodifiableList(new ArrayList<>(((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices()));
    }
    else if (selectionModel.isEmpty()) {
      return emptyList();
    }

    return singletonList(selectionModel.selectedIndexProperty().get());
  }

  @Override
  public void selectAll() {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).selectAll();
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void setSelectedItems(Predicate<Entity> predicate) {
    setSelectedIndexes(indexesToSelect(requireNonNull(predicate)));
  }

  @Override
  public void addSelectedItems(Predicate<Entity> predicate) {
    addSelectedIndexes(indexesToSelect(requireNonNull(predicate)));
  }

  @Override
  public void addSelectedIndexes(Collection<Integer> indexes) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().addAll(indexes);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public int selectionCount() {
    if (selectionModel instanceof MultipleSelectionModel) {
      return ((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().size();
    }

    return selectionModel.getSelectedIndex() == -1 ? 0 : 1;
  }

  @Override
  public void setSelectedItems(Collection<Entity> items) {
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

  @Override
  public List<Entity> getSelectedItems() {
    if (selectionModel instanceof MultipleSelectionModel) {
      return Collections.unmodifiableList(new ArrayList<>(((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems()));
    }
    else if (selectionModel.isEmpty()) {
      return emptyList();
    }

    return singletonList(selectionModel.getSelectedItem());
  }

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

  @Override
  public boolean isSelectedItem(Entity item) {
    requireNonNull(item);
    if (selectionModel instanceof MultipleSelectionModel) {
      return ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().contains(item);
    }

    return selectionModel.getSelectedItem().equals(item);
  }

  @Override
  public void setSelectedItem(Entity item) {
    if (selectionModel instanceof MultipleSelectionModel) {
      selectionModel.clearSelection();
    }
    selectionModel.select(item);
  }

  @Override
  public void addSelectedItem(Entity item) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().add(item);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void addSelectedItems(Collection<Entity> items) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().addAll(items);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void clearSelection() {
    selectionModel.clearSelection();
  }

  @Override
  public boolean isSelectionEmpty() {
    return selectionModel.isEmpty();
  }

  @Override
  public boolean isSelectionNotEmpty() {
    return !selectionModel.isEmpty();
  }

  private void bindEvents() {
    if (selectionModel instanceof MultipleSelectionModel) {
      MultipleSelectionModel<Entity> multipleSelectionModel = (MultipleSelectionModel<Entity>) this.selectionModel;
      multipleSelectionModel.getSelectedItems().addListener((ListChangeListener<Entity>) change -> {
        selectionEmptyState.set(this.selectionModel.isEmpty());
        List<Integer> selectedIndexes = getSelectedIndexes();
        singleSelectionState.set(selectedIndexes.size() == 1);
        multipleSelectionState.set(!selectionEmptyState.get() && !singleSelectionState.get());
        selectionEvent.onEvent();
        selectedIndexesEvent.onEvent(selectedIndexes);
        selectedItemEvent.onEvent(getSelectedItem());
        selectedItemsEvent.onEvent(getSelectedItems());
      });
      this.selectionModel.selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
        int newSelectedIndex = newValue.intValue();
        if (selectedIndex != newSelectedIndex) {
          selectedIndex = newSelectedIndex;
          selectedIndexEvent.onEvent(selectedIndex);
        }
      });
      multipleSelectionModel.selectionModeProperty().addListener((observable, oldValue, newValue) ->
              singleSelectionModeState.set(Objects.equals(SelectionMode.SINGLE, newValue)));
      singleSelectionModeState.addDataListener(singleSelection -> {
        SelectionMode newMode = singleSelection ? SelectionMode.SINGLE : SelectionMode.MULTIPLE;
        if (multipleSelectionModel.getSelectionMode() != newMode) {
          multipleSelectionModel.setSelectionMode(newMode);
        }
      });
    }
    else {
      selectionModel.selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
        int newSelectedIndex = newValue.intValue();
        if (selectedIndex != newSelectedIndex) {
          selectedIndex = newSelectedIndex;
          selectionEvent.onEvent();
          selectedIndexEvent.onEvent(selectedIndex);
          selectedIndexesEvent.onEvent(singletonList(selectedIndex));
          selectedItemEvent.onEvent(getSelectedItem());
          selectedItemsEvent.onEvent(getSelectedItems());
        }
      });
    }
  }

  private List<Integer> indexesToSelect(Predicate<Entity> predicate) {
    List<Integer> indexes = new ArrayList<>();
    List<Entity> visibleItems = filteredModel.visibleItems();
    for (int i = 0; i < visibleItems.size(); i++) {
      Entity item = visibleItems.get(i);
      if (predicate.test(item)) {
        indexes.add(i);
      }
    }

    return indexes;
  }
}
