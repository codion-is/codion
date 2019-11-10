/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.FilterCondition;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.swing.common.model.combobox.SwingFilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  private final FilterCondition<Entity> foreignKeyFilterCondition = item -> {
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
    setFilterCondition(foreignKeyFilterCondition);
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
  public final FilterCondition<Entity> getForeignKeyFilterCondition() {
    return foreignKeyFilterCondition;
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
    setFilterCondition(foreignKeyFilterCondition);
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
  public final EntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyId) {
    final ForeignKeyProperty foreignKeyProperty = domain.getDefinition(entityId).getForeignKeyProperty(foreignKeyPropertyId);
    final EntityComboBoxModel foreignKeyModel =
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
    final FilterCondition.RejectAllCondition rejectAllCondition = new FilterCondition.RejectAllCondition();
    if (isStrictForeignKeyFiltering()) {
      setFilterCondition(rejectAllCondition);
    }
    foreignKeyModel.addSelectionListener(selected -> {
      if (selected == null && isStrictForeignKeyFiltering()) {
        setFilterCondition(rejectAllCondition);
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
      refreshDoneEvent.fire();
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
      return connectionProvider.getConnection().selectMany(entitySelectCondition(entityId,
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

  private static final class ForeignKeyModelRefreshListener implements EventListener {

    private final EntityComboBoxModel foreignKeyModel;

    private ForeignKeyModelRefreshListener(final EntityComboBoxModel foreignKeyModel) {
      this.foreignKeyModel = foreignKeyModel;
    }

    @Override
    public void eventOccurred() {
      foreignKeyModel.forceRefresh();
    }
  }
}