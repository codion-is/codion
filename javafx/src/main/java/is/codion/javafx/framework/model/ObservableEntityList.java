/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static is.codion.framework.db.condition.Conditions.condition;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * An {@link ObservableList} based on a {@link Entity}
 */
public class ObservableEntityList extends SimpleListProperty<Entity> implements ObservableList<Entity>, FilteredModel<Entity> {

  private static final String SELECTION_MODEL_HAS_NOT_BEEN_SET = "Selection model has not been set";

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  private final EntityDefinition entityDefinition;
  private final SortedList<Entity> sortedList;
  private final FilteredList<Entity> filteredList;

  private final Event<?> refreshStartedEvent = Event.event();
  private final Event<Throwable> refreshFailedEvent = Event.event();
  private final Event<?> refreshEvent = Event.event();
  private final Event<?> selectionChangedEvent = Event.event();
  private final Event<?> filterEvent = Event.event();

  private FXEntityListSelectionModel selectionModel;

  private Condition selectCondition;
  private Predicate<Entity> includeCondition;
  private int queryRowCountLimit = -1;

  /**
   * Instantiates a new {@link ObservableEntityList}
   * @param entityType the entity on which to base the list
   * @param connectionProvider the connection provider
   */
  public ObservableEntityList(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
    super(FXCollections.observableArrayList());
    this.entityType = entityType;
    this.connectionProvider = connectionProvider;
    this.entityDefinition =  connectionProvider.getEntities().getDefinition(entityType);
    this.filteredList = new FilteredList<>(this);
    this.sortedList = new SortedList<>(filteredList, connectionProvider.getEntities().getDefinition(entityType).getComparator());
  }

  /**
   * @return the id of the underlying entity
   */
  public final EntityType getEntityType() {
    return entityType;
  }

  /**
   * @return the definition of the underlying entity
   */
  public final EntityDefinition getEntityDefinition() {
    return entityDefinition;
  }

  /**
   * @return the connection provider
   */
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final void refresh() {
    final List<Entity> selectedItems = getSelectedItems();
    onRefreshStarted();
    try {
      onRefreshResult(performQuery());
    }
    catch (final RuntimeException e) {
      onRefreshFailed(e);
    }
    catch (final Exception e) {
      onRefreshFailed(e);
    }
    setSelectedItems(selectedItems);
  }

  /**
   * Returns the query row count limit, a value of -1 means no limit.
   * @return the query row count limit
   */
  public final int getQueryRowCountLimit() {
    return queryRowCountLimit;
  }

  /**
   * Sets the query row count limit, a value of -1 means no limit.
   * @param queryRowCountLimit the query row count limit
   */
  public final void setQueryRowCountLimit(final int queryRowCountLimit) {
    this.queryRowCountLimit = queryRowCountLimit;
  }

  /**
   * @return a filtered view of this list
   * @see #setIncludeCondition(Predicate)
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

  @Override
  public final boolean isAsyncRefresh() {
    throw new UnsupportedOperationException("Async refresh is not supported");
  }

  @Override
  public final void setAsyncRefresh(final boolean asyncRefresh) {
    throw new UnsupportedOperationException("Async refresh is not supported");
  }

  /**
   * @param listener notified when the selection changes in the underlying selection model
   */
  public final void addRefreshListener(final EventListener listener) {
    refreshEvent.addListener(listener);
  }

  /**
   * @param listener notified each time this model is refreshed.
   */
  public final void removeRefreshListener(final EventListener listener) {
    refreshEvent.removeListener(listener);
  }

  /**
   * @return the selection model
   * @throws IllegalStateException in case the selection model has not been set
   */
  public final FXEntityListSelectionModel getSelectionModel() {
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

  /**
   * @param listener notified each time the selection changes.
   */
  public final void addSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.addListener(listener);
  }

  @Override
  public final boolean containsItem(final Entity item) {
    return filteredList.contains(item) || contains(item);
  }

  @Override
  public final boolean isVisible(final Entity item) {
    return filteredList.contains(item);
  }

  @Override
  public final boolean isFiltered(final Entity item) {
    return containsItem(item) && !filteredList.contains(item);
  }

  @Override
  public final List<Entity> getItems() {
    return FXCollections.unmodifiableObservableList(this);
  }

  @Override
  public final List<Entity> getFilteredItems() {
    if (size() != filteredList.size()) {
      final List<Entity> result = new ArrayList<>(this);
      result.removeAll(filteredList);

      return unmodifiableList(result);
    }

    return emptyList();
  }

  @Override
  public final List<Entity> getVisibleItems() {
    return FXCollections.unmodifiableObservableList(this);
  }

  @Override
  public final int getVisibleItemCount() {
    return filteredList.size();
  }

  @Override
  public final int getFilteredItemCount() {
    return size() - filteredList.size();
  }

  @Override
  public final Predicate<Entity> getIncludeCondition() {
    return includeCondition;
  }

  @Override
  public final void setIncludeCondition(final Predicate<Entity> includeCondition) {
    this.includeCondition = includeCondition;
    filterContents();
  }

  @Override
  public final void filterContents() {
    filteredList.setPredicate(entity -> includeCondition == null || includeCondition.test(entity));
    filterEvent.onEvent();
  }

  @Override
  public final void addFilterListener(final EventListener listener) {
    filterEvent.addListener(listener);
  }

  @Override
  public final void removeFilterListener(final EventListener listener) {
    filterEvent.removeListener(listener);
  }

  /**
   * @param listener a listener to be notified each time a refresh has failed
   * @see #refresh()
   */
  @Override
  public final void addRefreshFailedListener(final EventDataListener<Throwable> listener) {
    refreshFailedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  @Override
  public final void removeRefreshFailedListener(final EventDataListener<Throwable> listener) {
    refreshFailedEvent.removeDataListener(listener);
  }

  /**
   * Sets the condition to use when querying data
   * @param selectCondition the select condition to use
   * @see #performQuery()
   */
  public final void setSelectCondition(final Condition selectCondition) {
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
    checkQueryRowCount();
    try {
      Condition condition = selectCondition;
      if (condition == null) {
        condition = condition(entityType);
      }

      return connectionProvider.getConnection().select(condition.toSelectCondition()
              .orderBy(connectionProvider.getEntities().getDefinition(entityType).getOrderBy()));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the number of rows {@link #performQuery()} would return on next invocation
   */
  protected int getQueryRowCount() {
    Condition condition = selectCondition;
    if (condition == null) {
      condition = condition(entityType);
    }

    try {
      return connectionProvider.getConnection().rowCount(condition);
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

  private void onRefreshStarted() {
    refreshStartedEvent.onEvent();
  }

  private void onRefreshFailed(final Throwable throwable) {
    refreshFailedEvent.onEvent(throwable);
  }

  private void onRefreshResult(final Collection<Entity> items) {
    setAll(items);
    refreshEvent.onEvent();
  }

  private void setSelectedItems(final List<Entity> selectedItems) {
    if (selectedItems != null && selectionModel != null) {
      selectionModel.setSelectedItems(selectedItems);
    }
  }

  private List<Entity> getSelectedItems() {
    if (selectionModel != null) {
      return new ArrayList<>(selectionModel.getSelectedItems());
    }

    return null;
  }

  private void checkQueryRowCount() {
    if (queryRowCountLimit >= 0 && getQueryRowCount() > queryRowCountLimit) {
      throw new IllegalStateException("Too many rows returned, add query condition");
    }
  }

  private void checkIfSelectionModelHasBeenSet() {
    if (!selectionModelHasBeenSet()) {
      throw new IllegalStateException(SELECTION_MODEL_HAS_NOT_BEEN_SET);
    }
  }
}
