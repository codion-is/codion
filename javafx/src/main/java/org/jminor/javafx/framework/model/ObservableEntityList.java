/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.StateObserver;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.FilterCondition;
import org.jminor.common.model.FilteredModel;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.table.SelectionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ObservableEntityList extends SimpleListProperty<Entity>
        implements ObservableList<Entity>, FilteredModel<Entity>, Refreshable {

  private static final String SELECTION_MODEL_HAS_NOT_BEEN_SET = "Selection model has not been set";

  private final String entityID;
  private final EntityConnectionProvider connectionProvider;
  private final SortedList<Entity> sortedList;
  private final FilteredList<Entity> filteredList;

  private final Event refreshEvent = Events.event();
  private final Event selectionChangedEvent = Events.event();
  private final Event filteringDoneEvent = Events.event();

  private FXEntityListSelectionModel selectionModel;

  private EntitySelectCondition selectCondition;
  private FilterCondition<Entity> filterCondition;

  public ObservableEntityList(final String entityID, final EntityConnectionProvider connectionProvider) {
    super(FXCollections.observableArrayList());
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
    this.filteredList = new FilteredList<>(this);
    this.sortedList = new SortedList<>(filteredList, Entities.getComparator(entityID));
    this.selectCondition = EntityConditions.selectCondition(entityID, Entities.getOrderByClause(entityID));
  }

  public final String getEntityID() {
    return entityID;
  }

  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final void refresh() {
    List<Entity> selectedItems = null;
    if (selectionModel != null) {
      selectedItems = new ArrayList<>(selectionModel.getSelectedItems());
    }
    setAll(queryContents());
    refreshEvent.fire();
    if (selectedItems != null) {
      selectionModel.setSelectedItems(selectedItems);
    }
  }

  public final FilteredList<Entity> getFilteredList() {
    return filteredList;
  }

  public final SortedList<Entity> getSortedList() {
    return sortedList;
  }

  public final void setSelectionModel(final javafx.scene.control.SelectionModel<Entity> selectionModel) {
    if (this.selectionModel != null) {
      throw new IllegalStateException("Selection model has already been set");
    }
    this.selectionModel = new FXEntityListSelectionModel(Objects.requireNonNull(selectionModel));
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).setSelectionMode(SelectionMode.MULTIPLE);
    }
    bindSelectionModelEvents();
  }

  public final void addRefreshListener(final EventListener listener) {
    refreshEvent.addListener(listener);
  }

  public final SelectionModel<Entity> getSelectionModel() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel;
  }

  public final StateObserver getSelectionEmptyObserver() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel.getSelectionEmptyObserver();
  }

  public final StateObserver getSingleSelectionObserver() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel.getSingleSelectionObserver();
  }

  public final StateObserver getMultipleSelectionObserver() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel.getMultipleSelectionObserver();
  }

  /** {@inheritDoc} */
  public final void addSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean contains(final Entity item, final boolean includeFiltered) {
    if (includeFiltered) {
      return filteredList.contains(item) || contains(item);
    }

    return contains(item);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isVisible(final Entity item) {
    return filteredList.contains(item);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isFiltered(final Entity item) {
    return filterCondition != null && filterCondition.include(item);
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> getAllItems() {
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> getFilteredItems() {
    if (size() != filteredList.size()) {
      final List<Entity> result = new ArrayList<>(this);
      result.removeAll(filteredList);

      return result;
    }

    return Collections.emptyList();
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> getVisibleItems() {
    return Collections.unmodifiableList(this);
  }

  /** {@inheritDoc} */
  @Override
  public final int getVisibleItemCount() {
    return filteredList.size();
  }

  /** {@inheritDoc} */
  @Override
  public final int getFilteredItemCount() {
    return size() - filteredList.size();
  }

  /** {@inheritDoc} */
  public final FilterCondition<Entity> getFilterCondition() {
    return filterCondition;
  }

  /** {@inheritDoc} */
  public final void setFilterCondition(final FilterCondition<Entity> filterCondition) {
    this.filterCondition = filterCondition;
    filterContents();
  }

  /** {@inheritDoc} */
  @Override
  public final void filterContents() {
    filteredList.setPredicate(entity -> filterCondition == null || filterCondition.include(entity));
    filteringDoneEvent.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final void addFilteringListener(final EventListener listener) {
    filteringDoneEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeFilteringListener(final EventListener listener) {
    filteringDoneEvent.removeListener(listener);
  }

  public final void setEntitySelectCondition(final EntitySelectCondition entitySelectCondition) {
    if (entitySelectCondition != null && !entitySelectCondition.getEntityID().equals(entityID)) {
      throw new IllegalArgumentException("EntitySelectCondition entityID mismatch, " + entityID
              + " expected, got " + entitySelectCondition.getEntityID());
    }
    if (entitySelectCondition == null) {
      this.selectCondition = EntityConditions.selectCondition(entityID, Entities.getOrderByClause(entityID));
    }
    else {
      this.selectCondition = entitySelectCondition;
    }
  }

  protected List<Entity> queryContents() {
    try {
      return connectionProvider.getConnection().selectMany(selectCondition);
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  protected void bindSelectionModelEvents() {
    selectionModel.addSelectionChangedListener(selectionChangedEvent);
  }

  private void checkIfSelectionModelHasBeenSet() {
    if (selectionModel == null) {
      throw new IllegalStateException(SELECTION_MODEL_HAS_NOT_BEEN_SET);
    }
  }
}
