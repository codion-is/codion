/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.swing.common.model.combobox.SwingFilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 */
public class SwingEntityComboBoxModel extends SwingFilteredComboBoxModel<Entity> implements EntityComboBoxModel {

  private final Event refreshDoneEvent = Events.event();

  /**
   * the ID of the underlying entity
   */
  private final String entityID;

  /**
   * the EntityConnectionProvider instance used by this EntityComboBoxModel
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * true if the data should only be fetched once, unless <code>forceRefresh()</code> is called
   */
  private boolean staticData = false;

  /**
   * used to indicate that a refresh is being forced
   */
  private boolean forceRefresh = false;

  /**
   * the EntitySelectCriteria used to filter the data when queried
   */
  private EntitySelectCriteria selectCriteria;

  /**
   * A map of entities used to filter the contents of this model by foreign key value.
   * The key in the map is the ID of the relevant foreign key property.
   */
  private final Map<String, Set<Entity>> foreignKeyFilterEntities = new HashMap<>();

  private boolean strictForeignKeyFiltering = true;

  private final FilterCriteria<Entity> foreignKeyFilterCriteria = new FilterCriteria<Entity>() {
    @Override
    public boolean include(final Entity item) {
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
    }
  };

  /**
   * @param entityID the ID of the entity this combo box model should represent
   * @param connectionProvider a EntityConnectionProvider instance
   */
  public SwingEntityComboBoxModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
    this.selectCriteria = initializeSelectCriteria(entityID);
    setStaticData(Entities.isStaticData(entityID));
    final FilterCriteria<Entity> superCriteria = super.getFilterCriteria();
    setFilterCriteria(new FilterCriteria<Entity>() {
      @Override
      public boolean include(final Entity item) {
        return superCriteria.include(item) && foreignKeyFilterCriteria.include(item);
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + " [entityID: " + entityID + "]";
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final void forceRefresh() {
    try {
      forceRefresh = true;
      refresh();
    }
    finally  {
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
    for (final Entity entity : getAllItems()) {
      if (entity != null && entity.getKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedEntityByKey(final Entity.Key key) {
    Util.rejectNullValue(key, "key");
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
  public final void setEntitySelectCriteria(final EntitySelectCriteria entitySelectCriteria) {
    if (entitySelectCriteria != null && !entitySelectCriteria.getEntityID().equals(entityID)) {
      throw new IllegalArgumentException("EntitySelectCriteria entityID mismatch, " + entityID
              + " expected, got " + entitySelectCriteria.getEntityID());
    }
    if (entitySelectCriteria == null) {
      this.selectCriteria = initializeSelectCriteria(entityID);
    }
    else {
      this.selectCriteria = entitySelectCriteria;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setForeignKeyFilterEntities(final String foreignKeyPropertyID, final Collection<Entity> entities) {
    if (Util.nullOrEmpty(entities)) {
      foreignKeyFilterEntities.remove(foreignKeyPropertyID);
    }
    else {
      foreignKeyFilterEntities.put(foreignKeyPropertyID, new HashSet<>(entities));
    }

    filterContents();
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getForeignKeyFilterEntities(final String foreignKeyPropertyID) {
    final Collection<Entity> filterEntities = new ArrayList<>();
    if (foreignKeyFilterEntities.containsKey(foreignKeyPropertyID)) {
      filterEntities.addAll(foreignKeyFilterEntities.get(foreignKeyPropertyID));
    }

    return filterEntities;
  }

  /** {@inheritDoc} */
  @Override
  public void setStrictForeignKeyFiltering(final boolean strictForeignKeyFiltering) {
    this.strictForeignKeyFiltering = strictForeignKeyFiltering;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStrictForeignKeyFiltering() {
    return strictForeignKeyFiltering;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyID) {
    final Property.ForeignKeyProperty foreignKeyProperty = Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID);
    final EntityComboBoxModel foreignKeyModel =
            new SwingEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider);
    foreignKeyModel.setNullValue(EntityUtil.createToStringEntity(foreignKeyProperty.getReferencedEntityID(), "-"));
    foreignKeyModel.refresh();
    linkForeignKeyComboBoxModel(foreignKeyPropertyID, foreignKeyModel);

    return foreignKeyModel;
  }

  /** {@inheritDoc} */
  @Override
  public final void linkForeignKeyComboBoxModel(final String foreignKeyPropertyID, final EntityComboBoxModel foreignKeyModel) {
    final Property.ForeignKeyProperty foreignKeyProperty = Entities.getForeignKeyProperty(getEntityID(), foreignKeyPropertyID);
    if (!foreignKeyProperty.getReferencedEntityID().equals(foreignKeyModel.getEntityID())) {
      throw new IllegalArgumentException("Foreign key ComboBoxModel is of type: " + foreignKeyModel.getEntityID()
              + ", should be: " + foreignKeyProperty.getReferencedEntityID());
    }
    final Collection<Entity> filterEntities = getForeignKeyFilterEntities(foreignKeyPropertyID);
    if (!Util.nullOrEmpty(filterEntities)) {
      foreignKeyModel.setSelectedItem(filterEntities.iterator().next());
    }
    foreignKeyModel.addSelectionListener(new EventInfoListener<Entity>() {
      @Override
      public void eventOccurred(final Entity selected) {
        setForeignKeyFilterEntities(foreignKeyPropertyID,
                selected == null ? new ArrayList<>(0) : Collections.singletonList(selected));
      }
    });
    addSelectionListener(new EventInfoListener<Entity>() {
      @Override
      public void eventOccurred(final Entity selected) {
        if (selected != null) {
          foreignKeyModel.setSelectedEntityByKey(selected.getReferencedKey(foreignKeyProperty));
        }
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
    for (final Entity visibleItem : getVisibleItems()) {
      if (visibleItem != null && itemToString.equals(visibleItem.toString())) {
        return visibleItem;
      }
    }

    //item not found, select null value
    return null;
  }

  /** {@inheritDoc} */
  @Override
  protected final List<Entity> initializeContents() {
    try {
      if (staticData && !isCleared() && !forceRefresh) {
        return super.initializeContents();
      }

      return performQuery(selectCriteria);
    }
    finally {
      refreshDoneEvent.fire();
    }
  }

  /**
   * Retrieves the entities to present in this EntityComboBoxModel
   * @param selectCriteria the criteria to base the query on
   * @return the entities to present in this EntityComboBoxModel
   */
  protected List<Entity> performQuery(final EntitySelectCriteria selectCriteria) {
    Util.rejectNullValue(selectCriteria, "selectCriteria");
    try {
      return connectionProvider.getConnection().selectMany(selectCriteria);
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private EntitySelectCriteria initializeSelectCriteria(final String entityID) {
    return EntityCriteriaUtil.selectCriteria(entityID, Entities.getOrderByClause(entityID));
  }

  private int getIndexOfKey(final Entity.Key primaryKey) {
    final int size = getSize();
    for (int index = 0; index < size; index++) {
      final Object item = getElementAt(index);
      if (item != null && ((Entity) item).getKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private int getFilteredIndexOfKey(final Entity.Key primaryKey) {
    final List<Entity> filteredItems = getFilteredItems();
    for (int index = 0; index < filteredItems.size(); index++) {
      final Object item = filteredItems.get(index);
      if (((Entity) item).getKey().equals(primaryKey)) {
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