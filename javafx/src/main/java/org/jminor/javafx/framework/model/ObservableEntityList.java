/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.StateObserver;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.FilterCondition;
import org.jminor.common.model.FilteredModel;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.table.SelectionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * An {@link ObservableList} based on a {@link Entity}
 */
public class ObservableEntityList extends SimpleListProperty<Entity>
        implements ObservableList<Entity>, FilteredModel<Entity>, Refreshable {

  private static final String SELECTION_MODEL_HAS_NOT_BEEN_SET = "Selection model has not been set";

  private final String entityId;
  private final EntityConnectionProvider connectionProvider;
  private final SortedList<Entity> sortedList;
  private final FilteredList<Entity> filteredList;
  private final EntityConditions entityConditions;

  private final Event refreshEvent = Events.event();
  private final Event selectionChangedEvent = Events.event();
  private final Event filteringDoneEvent = Events.event();

  private FXEntityListSelectionModel selectionModel;

  private Condition<Property.ColumnProperty> selectCondition;
  private FilterCondition<Entity> filterCondition;

  /**
   * Instantiates a new {@link ObservableEntityList}
   * @param entityId the entity on which to base the list
   * @param connectionProvider the connection provider
   */
  public ObservableEntityList(final String entityId, final EntityConnectionProvider connectionProvider) {
    super(FXCollections.observableArrayList());
    this.entityId = entityId;
    this.connectionProvider = connectionProvider;
    this.filteredList = new FilteredList<>(this);
    this.sortedList = new SortedList<>(filteredList, connectionProvider.getDomain().getComparator(entityId));
    this.entityConditions = connectionProvider.getConditions();
  }

  /**
   * @return the ID of the underlying entity
   */
  public final String getEntityId() {
    return entityId;
  }

  /**
   * @return the connection provider
   */
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    List<Entity> selectedItems = null;
    if (selectionModel != null) {
      selectedItems = new ArrayList<>(selectionModel.getSelectedItems());
    }
    setAll(performQuery());
    refreshEvent.fire();
    if (selectedItems != null) {
      selectionModel.setSelectedItems(selectedItems);
    }
  }

  /**
   * @return a filtered view of this list
   * @see #setFilterCondition(FilterCondition)
   */
  public final FilteredList<Entity> getFilteredList() {
    return filteredList;
  }

  /**
   * @return a sorted view of this list
   */
  public final SortedList<Entity> getSortedList() {
    return sortedList;
  }

  /**
   * Sets the selection model to use
   * @param selectionModel the selection model
   * @throws IllegalStateException in case the selection model has already been set
   */
  public final void setSelectionModel(final javafx.scene.control.SelectionModel<Entity> selectionModel) {
    if (this.selectionModel != null) {
      throw new IllegalStateException("Selection model has already been set");
    }
    this.selectionModel = new FXEntityListSelectionModel(requireNonNull(selectionModel));
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).setSelectionMode(SelectionMode.MULTIPLE);
    }
    bindSelectionModelEvents();
  }

  /**
   * @param listener a listener notified each time this model is refreshed
   */
  public final void addRefreshListener(final EventListener listener) {
    refreshEvent.addListener(listener);
  }

  /**
   * @return the selection model
   * @throws IllegalStateException in case the selection model has not been set
   */
  public final SelectionModel<Entity> getSelectionModel() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel;
  }

  /**
   * @return a {@link StateObserver} active when the selection is empty
   * @throws IllegalStateException in case the selection model has not been set
   */
  public final StateObserver getSelectionEmptyObserver() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel.getSelectionEmptyObserver();
  }

  /**
   * @return a {@link StateObserver} active when a single item is selected
   * @throws IllegalStateException in case the selection model has not been set
   */
  public final StateObserver getSingleSelectionObserver() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel.getSingleSelectionObserver();
  }

  /**
   * @return a {@link StateObserver} active when multiple items are selected
   * @throws IllegalStateException in case the selection model has not been set
   */
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

    return emptyList();
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
  @Override
  public final FilterCondition<Entity> getFilterCondition() {
    return filterCondition;
  }

  /** {@inheritDoc} */
  @Override
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

  /**
   * Sets the condition to use when querying data
   * @param selectCondition the select condition to use
   * @see #performQuery()
   */
  public final void setSelectCondition(final Condition<Property.ColumnProperty> selectCondition) {
    this.selectCondition = selectCondition;
  }

  /**
   * @return true if the selection model has been set
   */
  protected final boolean selectionModelHasBeenSet() {
    return selectionModel != null;
  }

  /**
   * @return the entities to display in this list
   */
  protected List<Entity> performQuery() {
    try {
      return connectionProvider.getConnection().selectMany(entityConditions.selectCondition(entityId, selectCondition)
              .setOrderBy(connectionProvider.getDomain().getOrderBy(entityId)));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Binds model events to the selection model
   */
  protected void bindSelectionModelEvents() {
    selectionModel.addSelectionChangedListener(selectionChangedEvent);
  }

  private void checkIfSelectionModelHasBeenSet() {
    if (!selectionModelHasBeenSet()) {
      throw new IllegalStateException(SELECTION_MODEL_HAS_NOT_BEEN_SET);
    }
  }
}
