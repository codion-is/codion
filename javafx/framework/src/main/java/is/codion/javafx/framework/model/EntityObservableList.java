/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static is.codion.framework.db.condition.Condition.condition;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * An {@link ObservableList} based on a {@link Entity}
 */
public class EntityObservableList extends SimpleListProperty<Entity> implements ObservableList<Entity>, FilteredModel<Entity> {

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
  private final State refreshingState = State.state();

  private FXEntityListSelectionModel selectionModel;

  private Condition selectCondition;
  private Predicate<Entity> includeCondition;
  private boolean asyncRefresh = FilteredModel.ASYNC_REFRESH.get();

  /**
   * Instantiates a new {@link EntityObservableList}
   * @param entityType the entity on which to base the list
   * @param connectionProvider the connection provider
   */
  public EntityObservableList(EntityType entityType, EntityConnectionProvider connectionProvider) {
    super(FXCollections.observableArrayList());
    this.entityType = requireNonNull(entityType);
    this.connectionProvider = requireNonNull(connectionProvider);
    this.entityDefinition = connectionProvider.entities().definition(entityType);
    this.filteredList = new FilteredList<>(this);
    this.sortedList = new SortedList<>(filteredList, connectionProvider.entities().definition(entityType).comparator());
  }

  /**
   * @return the id of the underlying entity
   */
  public final EntityType entityType() {
    return entityType;
  }

  /**
   * @return the definition of the underlying entity
   */
  public final EntityDefinition entityDefinition() {
    return entityDefinition;
  }

  /**
   * @return the connection provider
   */
  public final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  @Override
  public final void refresh() {
    refreshThen(null);
  }

  @Override
  public final void refreshThen(Consumer<Collection<Entity>> afterRefresh) {
    if (asyncRefresh && Platform.isFxApplicationThread()) {
      refreshAsync(afterRefresh);
    }
    else {
      refreshSync(afterRefresh);
    }
  }

  @Override
  public final StateObserver refreshingObserver() {
    return refreshingState.observer();
  }

  @Override
  public final boolean isAsyncRefresh() {
    return asyncRefresh;
  }

  @Override
  public final void setAsyncRefresh(boolean asyncRefresh) {
    this.asyncRefresh = asyncRefresh;
  }

  /**
   * @return a filtered view of this list
   * @see #setIncludeCondition(Predicate)
   */
  public final FilteredList<Entity> filteredList() {
    return filteredList;
  }

  /**
   * @return a sorted view of this list
   */
  public final SortedList<Entity> sortedList() {
    return sortedList;
  }

  /**
   * Sets the selection model to use
   * @param selectionModel the selection model
   * @throws IllegalStateException in case the selection model has already been set
   */
  public final void setSelectionModel(javafx.scene.control.SelectionModel<Entity> selectionModel) {
    if (this.selectionModel != null) {
      throw new IllegalStateException("Selection model has already been set");
    }
    this.selectionModel = new FXEntityListSelectionModel(this, requireNonNull(selectionModel));
    if (selectionModel instanceof MultipleSelectionModel) {
      ((MultipleSelectionModel<Entity>) selectionModel).setSelectionMode(SelectionMode.MULTIPLE);
    }
    bindSelectionModelEvents();
  }

  /**
   * @param listener notified when the selection changes in the underlying selection model
   */
  public final void addRefreshListener(EventListener listener) {
    refreshEvent.addListener(listener);
  }

  /**
   * @param listener notified each time this model is refreshed.
   */
  public final void removeRefreshListener(EventListener listener) {
    refreshEvent.removeListener(listener);
  }

  /**
   * @return the selection model
   * @throws IllegalStateException in case the selection model has not been set
   */
  public final FXEntityListSelectionModel selectionModel() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel;
  }

  /**
   * @return the selection model, or an empty optional if the selection model has not been set
   */
  public final Optional<FXEntityListSelectionModel> selectionModelOptional() {
    return Optional.ofNullable(selectionModel);
  }

  /**
   * @return a {@link StateObserver} active when the selection is empty
   * @throws IllegalStateException in case the selection model has not been set
   */
  public final StateObserver selectionEmptyObserver() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel.selectionEmptyObserver();
  }

  /**
   * @return a {@link StateObserver} active when a single item is selected
   * @throws IllegalStateException in case the selection model has not been set
   */
  public final StateObserver singleSelectionObserver() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel.singleSelectionObserver();
  }

  /**
   * @return a {@link StateObserver} active when multiple items are selected
   * @throws IllegalStateException in case the selection model has not been set
   */
  public final StateObserver multipleSelectionObserver() {
    checkIfSelectionModelHasBeenSet();
    return selectionModel.multipleSelectionObserver();
  }

  /**
   * @param listener notified each time the selection changes.
   */
  public final void addSelectionListener(EventListener listener) {
    selectionChangedEvent.addListener(listener);
  }

  @Override
  public final boolean containsItem(Entity item) {
    return filteredList.contains(item) || contains(item);
  }

  @Override
  public final boolean isVisible(Entity item) {
    return filteredList.contains(item);
  }

  @Override
  public final boolean isFiltered(Entity item) {
    return containsItem(item) && !filteredList.contains(item);
  }

  @Override
  public final List<Entity> items() {
    return FXCollections.unmodifiableObservableList(this);
  }

  @Override
  public final List<Entity> filteredItems() {
    if (size() != filteredList.size()) {
      List<Entity> result = new ArrayList<>(this);
      result.removeAll(filteredList);

      return unmodifiableList(result);
    }

    return emptyList();
  }

  @Override
  public final List<Entity> visibleItems() {
    return FXCollections.unmodifiableObservableList(this);
  }

  @Override
  public final int visibleItemCount() {
    return filteredList.size();
  }

  @Override
  public final int filteredItemCount() {
    return size() - filteredList.size();
  }

  @Override
  public final Predicate<Entity> getIncludeCondition() {
    return includeCondition;
  }

  @Override
  public final void setIncludeCondition(Predicate<Entity> includeCondition) {
    this.includeCondition = includeCondition;
    filterItems();
  }

  @Override
  public final void filterItems() {
    filteredList.setPredicate(entity -> includeCondition == null || includeCondition.test(entity));
    filterEvent.onEvent();
  }

  @Override
  public final boolean add(Entity element) {
    validate(element);
    return super.add(element);
  }

  @Override
  public final boolean addAll(Collection<? extends Entity> elements) {
    requireNonNull(elements).forEach(this::validate);
    return super.addAll(elements);
  }

  @Override
  public final boolean addAll(int i, Collection<? extends Entity> elements) {
    requireNonNull(elements).forEach(this::validate);
    return super.addAll(i, elements);
  }

  @Override
  public final void add(int i, Entity element) {
    validate(element);
    super.add(i, element);
  }

  @Override
  public final boolean addAll(Entity... elements) {
    Stream.of(elements).forEach(this::validate);
    return super.addAll(elements);
  }

  @Override
  public final void set(ObservableList<Entity> list) {
    requireNonNull(list);
    if (list instanceof EntityObservableList && !((EntityObservableList) list).entityType.equals(entityType)) {
      throw new IllegalArgumentException("List is not compatible: " + list);
    }
    requireNonNull(list).forEach(this::validate);
    super.set(list);
  }

  @Override
  public final boolean setAll(Entity... elements) {
    Stream.of(elements).forEach(this::validate);
    return super.setAll(elements);
  }

  @Override
  public final boolean setAll(Collection<? extends Entity> elements) {
    requireNonNull(elements).forEach(this::validate);
    return super.setAll(elements);
  }

  /**
   * @param listener a listener to be notified each time a refresh has failed
   * @see #refresh()
   */
  @Override
  public final void addRefreshFailedListener(EventDataListener<Throwable> listener) {
    refreshFailedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  @Override
  public final void removeRefreshFailedListener(EventDataListener<Throwable> listener) {
    refreshFailedEvent.removeDataListener(listener);
  }

  /**
   * Sets the condition to use when querying data
   * @param selectCondition the select condition to use
   * @see #performQuery()
   */
  public final void setSelectCondition(Condition selectCondition) {
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
      Condition condition = selectCondition;
      if (condition == null) {
        condition = condition(entityType);
      }

      return connectionProvider.connection().select(condition.selectBuilder()
              .orderBy(connectionProvider.entities().definition(entityType).orderBy())
              .build());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param entity the entity to validate
   * @return true if the entity is of the correct type
   */
  protected boolean validItem(Entity entity) {
    return entity.type().equals(entityType);
  }

  /**
   * Binds model events to the selection model
   */
  protected void bindSelectionModelEvents() {
    selectionModel.addSelectionListener(selectionChangedEvent);
  }

  private void validate(Entity entity) {
    requireNonNull(entity);
    if (!validItem(entity)) {
      throw new IllegalArgumentException("Invalid item: " + entity);
    }
  }

  private void refreshAsync(Consumer<Collection<Entity>> afterRefresh) {
    new Thread(new RefreshTask(getSelectedItems(), afterRefresh)).start();
  }

  private void refreshSync(Consumer<Collection<Entity>> afterRefresh) {
    List<Entity> selectedItems = getSelectedItems();
    onRefreshStarted();
    try {
      onRefreshResult(performQuery(), afterRefresh);
    }
    catch (Exception e) {
      onRefreshFailedSync(e);
    }
    setSelectedItems(selectedItems);
  }

  private void onRefreshStarted() {
    refreshingState.set(true);
    refreshStartedEvent.onEvent();
  }

  private void onRefreshFailedSync(Throwable throwable) {
    refreshingState.set(false);
    if (throwable instanceof RuntimeException) {
      throw (RuntimeException) throwable;
    }

    throw new RuntimeException(throwable);
  }

  private void onRefreshResult(Collection<Entity> items, Consumer<Collection<Entity>> afterRefresh) {
    setAll(items);
    refreshingState.set(false);
    if (afterRefresh != null) {
      afterRefresh.accept(items);
    }
    refreshEvent.onEvent();
  }

  private void setSelectedItems(List<Entity> selectedItems) {
    if (selectedItems != null && selectionModel != null) {
      selectionModel.setSelectedItems(selectedItems);
    }
  }

  private List<Entity> getSelectedItems() {
    if (selectionModel != null) {
      return new ArrayList<>(selectionModel.getSelectedItems());
    }

    return emptyList();
  }

  private void checkIfSelectionModelHasBeenSet() {
    if (!selectionModelHasBeenSet()) {
      throw new IllegalStateException(SELECTION_MODEL_HAS_NOT_BEEN_SET);
    }
  }

  private final class RefreshTask extends Task<Collection<Entity>> {

    private final List<Entity> selectedItems;
    private final Consumer<Collection<Entity>> afterRefresh;

    private RefreshTask(List<Entity> selectedItems, Consumer<Collection<Entity>> afterRefresh) {
      this.selectedItems = selectedItems;
      this.afterRefresh = afterRefresh;
    }

    @Override
    protected Collection<Entity> call() throws Exception {
      return performQuery();
    }

    @Override
    protected void running() {
      onRefreshStarted();
    }

    @Override
    protected void failed() {
      refreshingState.set(false);
      refreshFailedEvent.onEvent(getException());
    }

    @Override
    protected void succeeded() {
      onRefreshResult(getValue(), afterRefresh);
      setSelectedItems(selectedItems);
    }
  }
}
