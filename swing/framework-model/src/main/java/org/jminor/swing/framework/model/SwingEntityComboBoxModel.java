/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;
import org.jminor.common.value.AbstractValue;
import org.jminor.common.value.Value;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityEditEvents;
import org.jminor.swing.common.model.combobox.SwingFilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 */
public class SwingEntityComboBoxModel extends SwingFilteredComboBoxModel<Entity> implements EntityComboBoxModel {

  private final Event refreshDoneEvent = Events.event();

  /**
   * the ID of the underlying entity
   */
  private final String entityId;

  /**
   * the EntityConnectionProvider instance used by this EntityComboBoxModel
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * The domain model
   */
  private final Domain domain;

  /**
   * true if the data should only be fetched once, unless {@code forceRefresh()} is called
   */
  private boolean staticData = false;

  /**
   * used to indicate that a refresh is being forced, as in, overriding the staticData directive
   */
  private boolean forceRefresh = false;

  /**
   * the Condition.Provider used when querying
   */
  private Condition.Provider selectConditionProvider;

  /**
   * A map of entities used to filter the contents of this model by foreign key value.
   * The key in the map is the propertyId of the foreign key property.
   */
  private final Map<String, Set<Entity>> foreignKeyFilterEntities = new HashMap<>();

  private boolean strictForeignKeyFiltering = true;

  private boolean listenToEditEvents = true;

  //we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
  private final EventDataListener<List<Entity>> insertListener = new InsertListener();
  private final EventDataListener<Map<Entity.Key, Entity>> updateListener = new UpdateListener();
  private final EventDataListener<List<Entity>> deleteListener = new DeleteListener();

  private final Predicate<Entity> foreignKeyIncludeCondition = item -> {
    for (final Map.Entry<String, Set<Entity>> entry : foreignKeyFilterEntities.entrySet()) {
      final Entity foreignKeyValue = item.getForeignKey(entry.getKey());
      if (foreignKeyValue == null) {
        return !strictForeignKeyFiltering;
      }
      if (!entry.getValue().contains(foreignKeyValue)) {
        return false;
      }
    }

    return true;
  };

  /**
   * @param entityId the id of the entity this combo box model should represent
   * @param connectionProvider a EntityConnectionProvider instance
   */
  public SwingEntityComboBoxModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    requireNonNull(entityId, "entityId");
    requireNonNull(connectionProvider, "connectionProvider");
    this.entityId = entityId;
    this.connectionProvider = connectionProvider;
    this.domain = connectionProvider.getDomain();
    setStaticData(this.domain.getDefinition(entityId).isStaticData());
    setIncludeCondition(foreignKeyIncludeCondition);
    addEditEventListeners();
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + " [entityId: " + entityId + "]";
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final void forceRefresh() {
    try {
      forceRefresh = true;
      refresh();
    }
    finally {
      forceRefresh = false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isStaticData() {
    return staticData;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityComboBoxModel setStaticData(final boolean staticData) {
    this.staticData = staticData;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isListenToEditEvents() {
    return listenToEditEvents;
  }

  /** {@inheritDoc} */
  @Override
  public EntityComboBoxModel setListenToEditEvents(final boolean listenToEditEvents) {
    this.listenToEditEvents = listenToEditEvents;
    if (listenToEditEvents) {
      addEditEventListeners();
    }
    else {
      removeEditEventListeners();
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getEntity(final Entity.Key primaryKey) {
    return getAllItems().stream().filter(entity -> entity != null && entity.getKey().equals(primaryKey)).findFirst().orElse(null);
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedEntityByKey(final Entity.Key key) {
    requireNonNull(key, "key");
    final int indexOfKey = getIndexOfKey(key);
    if (indexOfKey >= 0) {
      setSelectedItem(getElementAt(indexOfKey));
    }
    else {
      final int filteredIndexOfKey = getFilteredIndexOfKey(key);
      if (filteredIndexOfKey >= 0) {
        setSelectedItem(getFilteredItems().get(filteredIndexOfKey));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectConditionProvider(final Condition.Provider selectConditionProvider) {
    this.selectConditionProvider = selectConditionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final Condition.Provider getSelectConditionProvider() {
    return this.selectConditionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final Predicate<Entity> getForeignKeyIncludeCondition() {
    return foreignKeyIncludeCondition;
  }

  /** {@inheritDoc} */
  @Override
  public final void setForeignKeyFilterEntities(final String foreignKeyPropertyId, final Collection<Entity> entities) {
    if (Util.nullOrEmpty(entities)) {
      foreignKeyFilterEntities.remove(foreignKeyPropertyId);
    }
    else {
      foreignKeyFilterEntities.put(foreignKeyPropertyId, new HashSet<>(entities));
    }
    setIncludeCondition(foreignKeyIncludeCondition);
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getForeignKeyFilterEntities(final String foreignKeyPropertyId) {
    final Collection<Entity> filterEntities = new ArrayList<>();
    if (foreignKeyFilterEntities.containsKey(foreignKeyPropertyId)) {
      filterEntities.addAll(foreignKeyFilterEntities.get(foreignKeyPropertyId));
    }

    return filterEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final void setStrictForeignKeyFiltering(final boolean strictForeignKeyFiltering) {
    this.strictForeignKeyFiltering = strictForeignKeyFiltering;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isStrictForeignKeyFiltering() {
    return strictForeignKeyFiltering;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyId) {
    final ForeignKeyProperty foreignKeyProperty = domain.getDefinition(entityId).getForeignKeyProperty(foreignKeyPropertyId);
    final SwingEntityComboBoxModel foreignKeyModel =
            new SwingEntityComboBoxModel(foreignKeyProperty.getForeignEntityId(), connectionProvider);
    foreignKeyModel.setNullValue(domain.createToStringEntity(foreignKeyProperty.getForeignEntityId(), "-"));
    foreignKeyModel.refresh();
    linkForeignKeyComboBoxModel(foreignKeyPropertyId, foreignKeyModel);

    return foreignKeyModel;
  }

  /** {@inheritDoc} */
  @Override
  public final void linkForeignKeyComboBoxModel(final String foreignKeyPropertyId, final EntityComboBoxModel foreignKeyModel) {
    final ForeignKeyProperty foreignKeyProperty = domain.getDefinition(entityId).getForeignKeyProperty(foreignKeyPropertyId);
    if (!foreignKeyProperty.getForeignEntityId().equals(foreignKeyModel.getEntityId())) {
      throw new IllegalArgumentException("Foreign key ComboBoxModel is of type: " + foreignKeyModel.getEntityId()
              + ", should be: " + foreignKeyProperty.getForeignEntityId());
    }
    final Collection<Entity> filterEntities = getForeignKeyFilterEntities(foreignKeyPropertyId);
    if (!Util.nullOrEmpty(filterEntities)) {
      foreignKeyModel.setSelectedItem(filterEntities.iterator().next());
    }
    final Predicate<Entity> filterAllCondition = item -> false;
    if (isStrictForeignKeyFiltering()) {
      setIncludeCondition(filterAllCondition);
    }
    foreignKeyModel.addSelectionListener(selected -> {
      if (selected == null && isStrictForeignKeyFiltering()) {
        setIncludeCondition(filterAllCondition);
      }
      else {
        setForeignKeyFilterEntities(foreignKeyPropertyId, selected == null ? emptyList() : singletonList(selected));
      }
    });
    addSelectionListener(selected -> {
      if (selected != null) {
        foreignKeyModel.setSelectedEntityByKey(selected.getReferencedKey(foreignKeyProperty));
      }
    });
    addRefreshListener(new ForeignKeyModelRefreshListener(foreignKeyModel));
  }

  /** {@inheritDoc} */
  @Override
  public final Value<Integer> integerValueSelector(final String propertyId) {
    return integerValueSelector(propertyId, (entities, thePropertyId, value) ->
            entities.stream().filter(entity ->
                    Objects.equals(value, entity.get(propertyId))).findFirst().orElse(null));
  }

  /** {@inheritDoc} */
  @Override
  public final Value<Integer> integerValueSelector(final String propertyId, final Finder<Integer> finder) {
    return new SelectorValue<>(propertyId, finder);
  }

  /** {@inheritDoc} */
  @Override
  public final void addRefreshListener(final EventListener listener) {
    refreshDoneEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeRefreshListener(final EventListener listener) {
    refreshDoneEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  protected final Entity translateSelectionItem(final Object item) {
    if (item == null) {
      return null;
    }

    if (item instanceof Entity) {
      final int indexOfKey = getIndexOfKey(((Entity) item).getKey());
      if (indexOfKey >= 0) {
        return getElementAt(indexOfKey);
      }

      return (Entity) item;
    }
    final String itemToString = item.toString();

    //item not found, select null value
    return getVisibleItems().stream().filter(visibleItem ->
            visibleItem != null && itemToString.equals(visibleItem.toString())).findFirst().orElse(null);
  }

  /** {@inheritDoc} */
  @Override
  protected final List<Entity> initializeContents() {
    try {
      if (staticData && !isCleared() && !forceRefresh) {
        return super.initializeContents();
      }

      return performQuery();
    }
    finally {
      refreshDoneEvent.onEvent();
    }
  }

  /**
   * Retrieves the entities to present in this EntityComboBoxModel, taking into account
   * the {@link #getSelectConditionProvider()} specified for this model
   * @return the entities to present in this EntityComboBoxModel
   * @see #getSelectConditionProvider()
   */
  protected List<Entity> performQuery() {
    try {
      return connectionProvider.getConnection().select(entitySelectCondition(entityId,
              selectConditionProvider == null ? null : selectConditionProvider.getCondition()));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private int getIndexOfKey(final Entity.Key primaryKey) {
    final int size = getSize();
    for (int index = 0; index < size; index++) {
      final Entity item = getElementAt(index);
      if (item != null && item.getKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private int getFilteredIndexOfKey(final Entity.Key primaryKey) {
    final List<Entity> filteredItems = getFilteredItems();
    for (int index = 0; index < filteredItems.size(); index++) {
      final Entity item = filteredItems.get(index);
      if (item.getKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private void addEditEventListeners() {
    EntityEditEvents.addInsertListener(entityId, insertListener);
    EntityEditEvents.addUpdateListener(entityId, updateListener);
    EntityEditEvents.addDeleteListener(entityId, deleteListener);
  }

  private void removeEditEventListeners() {
    EntityEditEvents.removeInsertListener(entityId, insertListener);
    EntityEditEvents.removeUpdateListener(entityId, updateListener);
    EntityEditEvents.removeDeleteListener(entityId, deleteListener);
  }

  private final class InsertListener implements EventDataListener<List<Entity>> {

    @Override
    public void onEvent(final List<Entity> inserted) {
      inserted.forEach(SwingEntityComboBoxModel.this::addItem);
    }
  }

  private final class UpdateListener implements EventDataListener<Map<Entity.Key, Entity>> {

    @Override
    public void onEvent(final Map<Entity.Key, Entity> updated) {
      final Domain domainModel = getConnectionProvider().getDomain();
      updated.forEach((key, entity) -> replaceItem(domainModel.entity(key), entity));
    }
  }

  private final class DeleteListener implements EventDataListener<List<Entity>> {

    @Override
    public void onEvent(final List<Entity> deleted) {
      deleted.forEach(SwingEntityComboBoxModel.this::removeItem);
    }
  }

  private static final class ForeignKeyModelRefreshListener implements EventListener {

    private final EntityComboBoxModel foreignKeyModel;

    private ForeignKeyModelRefreshListener(final EntityComboBoxModel foreignKeyModel) {
      this.foreignKeyModel = foreignKeyModel;
    }

    @Override
    public void onEvent() {
      foreignKeyModel.forceRefresh();
    }
  }

  private final class SelectorValue<T> extends AbstractValue<T> {

    private final String propertyId;
    private final EntityComboBoxModel.Finder<T> finder;

    /**
     * @param propertyId the property
     * @param finder the Finder instance responsible for finding the entity by value
     */
    private SelectorValue(final String propertyId, final EntityComboBoxModel.Finder<T> finder) {
      this.propertyId = requireNonNull(propertyId);
      this.finder = requireNonNull(finder);
      addSelectionListener(selected -> notifyValueChange());
    }

    /**
     * Selects the first entity found in the underlying combo box model, which
     * has the the given value associated with the underlying property.
     * @param value the value
     */
    @Override
    public void set(final T value) {
      setSelectedItem(value == null ? null : finder.findByValue(getVisibleItems(), propertyId, value));
    }

    /**
     * @return the value of the underlying property in the selected Entity, null if the selection is empty
     */
    @Override
    public T get() {
      if (isSelectionEmpty()) {
        return null;
      }

      return (T) getSelectedValue().get(propertyId);
    }

    /**
     * @return true
     */
    @Override
    public boolean isNullable() {
      return true;
    }
  }
}