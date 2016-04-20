package org.jminor.javafx.framework.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.table.SelectionModel;
import org.jminor.framework.domain.Entity;

import javafx.collections.ListChangeListener;
import javafx.scene.control.MultipleSelectionModel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class FXEntityListSelectionModel implements SelectionModel<Entity> {

  private final Event selectionChangedEvent = Events.event();
  private final Event selectedIndexChangedEvent = Events.event();
  private final State selectionEmptyState = States.state(true);
  private final State multipleSelectionState = States.state(false);
  private final State singleSelectionState = States.state(false);

  private final javafx.scene.control.SelectionModel<Entity> selectionModel;
  private int selectedIndex = -1;

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

  public javafx.scene.control.SelectionModel<Entity> getSelectionModel() {
    return selectionModel;
  }

  @Override
  public StateObserver getSelectionEmptyObserver() {
    return selectionEmptyState.getObserver();
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
  public void addSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.addListener(listener);
  }

  @Override
  public void removeSelectionChangedListener(final EventListener listener) {

  }

  @Override
  public void addSelectedIndexListener(final EventListener listener) {
    selectedIndexChangedEvent.addListener(listener);
  }

  @Override
  public void removeSelectedIndexListener(final EventListener listener) {
    selectedIndexChangedEvent.removeListener(listener);
  }

  @Override
  public void moveSelectionDown() {

  }

  @Override
  public void moveSelectionUp() {

  }

  @Override
  public int getSelectedIndex() {
    return selectedIndex;
  }

  @Override
  public void addSelectedIndex(final int index) {
    selectionModel.selectedIndexProperty().add(index);
  }

  @Override
  public void setSelectedIndex(final int index) {
    selectionModel.selectedIndexProperty().add(index);
  }

  @Override
  public void setSelectedIndexes(final Collection<Integer> indexes) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().setAll(indexes);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public List<Integer> getSelectedIndexes() {
    if (selectionModel instanceof MultipleSelectionModel) {
      return Collections.unmodifiableList(((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices());
    }
    else if (selectionModel.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.singletonList(selectionModel.selectedIndexProperty().get());
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
  public void addSelectedIndexes(final Collection<Integer> indexes) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().addAll(indexes);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public int getSelectionCount() {
    if (selectionModel instanceof MultipleSelectionModel) {
      return ((MultipleSelectionModel<Entity>) selectionModel).getSelectedIndices().size();
    }

    return selectionModel.getSelectedIndex() == -1 ? 0 : 1;
  }

  @Override
  public void setSelectedItems(final Collection<Entity> items) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().setAll(items);
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
      return Collections.unmodifiableList(((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems());
    }
    else if (selectionModel.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.singletonList(selectionModel.getSelectedItem());
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
  public void setSelectedItem(final Entity item) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().setAll(item);
    }
    else {
      selectionModel.select(item);
    }
  }

  @Override
  public void addSelectedItem(final Entity item) {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().add(item);
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void addSelectedItems(final Collection<Entity> items) {
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

  private void bindEvents() {
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).getSelectedItems().addListener((ListChangeListener<Entity>) change -> {
        selectionEmptyState.setActive(selectionModel.isEmpty());
        final List<Integer> selectedIndices = getSelectedIndexes();
        singleSelectionState.setActive(selectedIndices.size() == 1);
        multipleSelectionState.setActive(!selectionEmptyState.isActive() && !singleSelectionState.isActive());
        selectionChangedEvent.fire();
      });
    }
    selectionModel.selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
      final int newSelectedIndex = newValue.intValue();
      if (selectedIndex != newSelectedIndex) {
        selectedIndex = newSelectedIndex;
        selectedIndexChangedEvent.fire();
      }
    });
  }
}
