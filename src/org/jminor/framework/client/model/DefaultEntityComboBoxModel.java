/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.DefaultFilteredComboBoxModel;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 */
public class DefaultEntityComboBoxModel extends DefaultFilteredComboBoxModel<Entity> implements EntityComboBoxModel {

  private final Event evtRefreshDone = Events.event();

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
  private final Map<String, Set<Entity>> foreignKeyFilterEntities = new HashMap<String, Set<Entity>>();

  private final FilterCriteria<Entity> foreignKeyFilterCriteria = new FilterCriteria<Entity>() {
    /** {@inheritDoc} */
    @Override
    public boolean include(final Entity item) {
      for (final Map.Entry<String, Set<Entity>> entry : foreignKeyFilterEntities.entrySet()) {
        final Entity foreignKeyValue = item.getForeignKeyValue(entry.getKey());
        final Set<Entity> filterValues = entry.getValue();
        if (foreignKeyValue != null && !filterValues.isEmpty() && !filterValues.contains(foreignKeyValue)) {
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
  public DefaultEntityComboBoxModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
    this.selectCriteria = initializeSelectCriteria(entityID);
    final FilterCriteria<Entity> superCriteria = super.getFilterCriteria();
    setFilterCriteria(new FilterCriteria<Entity>() {
      /** {@inheritDoc} */
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
      if (entity != null && entity.getPrimaryKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedEntityByPrimaryKey(final Entity.Key primaryKey) {
    Util.rejectNullValue(primaryKey, "primaryKey");
    final int indexOfKey = getIndexOfKey(primaryKey);
    if (indexOfKey >= 0) {
      setSelectedItem(getElementAt(indexOfKey));
    }
    else {
      final int filteredIndexOfKey = getFilteredIndexOfKey(primaryKey);
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
    final Set<Entity> filterEntities = new HashSet<Entity>();
    if (entities != null) {
      filterEntities.addAll(entities);
    }
    foreignKeyFilterEntities.put(foreignKeyPropertyID, filterEntities);

    filterContents();
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getForeignKeyFilterEntities(final String foreignKeyPropertyID) {
    final Collection<Entity> filterEntities = new ArrayList<Entity>();
    if (foreignKeyFilterEntities.containsKey(foreignKeyPropertyID)) {
      filterEntities.addAll(foreignKeyFilterEntities.get(foreignKeyPropertyID));
    }

    return filterEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyID) {
    final Property.ForeignKeyProperty foreignKeyProperty = Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID);
    final EntityComboBoxModel foreignKeyModel =
            new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider);
    foreignKeyModel.setNullValueString("-");
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
    foreignKeyModel.addSelectionListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        final Entity selectedEntity = foreignKeyModel.getSelectedValue();
        setForeignKeyFilterEntities(foreignKeyPropertyID,
                selectedEntity == null ? new ArrayList<Entity>(0) : Arrays.asList(selectedEntity));
      }
    });
    addSelectionListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        final Entity selected = getSelectedValue();
        if (selected != null) {
          foreignKeyModel.setSelectedEntityByPrimaryKey(selected.getReferencedPrimaryKey(foreignKeyProperty));
        }
      }
    });
    addRefreshListener(new ForeignKeyModelRefreshListener(foreignKeyModel));
  }

  /** {@inheritDoc} */
  @Override
  public final void addRefreshListener(final EventListener listener) {
    evtRefreshDone.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeRefreshListener(final EventListener listener) {
    evtRefreshDone.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  protected Object translateSelectionItem(final Object item) {
    if (item instanceof Entity) {
      final int indexOfKey = getIndexOfKey(((Entity) item).getPrimaryKey());
      if (indexOfKey >= 0) {
        return getElementAt(indexOfKey);
      }
      else {
        return item;
      }
    }
    else {
      return item;
    }
  }

  /**
   * Returns true if the given item can not be selected, that is, if the item is not null, not the nullValueString and not an Entity instance.
   * @param item the item to be selected
   * @return true if the item can not be selected in this model
   */
  @Override
  protected final boolean vetoSelectionChange(final Object item) {
    final Object theItem = item instanceof String && ((String) item).isEmpty() ? null : item;

    return theItem != null && !theItem.equals(getNullValueString()) && !(theItem instanceof Entity);
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
      evtRefreshDone.fire();
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
    catch (DatabaseException e) {
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
      if (item instanceof Entity && ((Entity) item).getPrimaryKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private int getFilteredIndexOfKey(final Entity.Key primaryKey) {
    final List<Entity> filteredItems = getFilteredItems();
    for (int index = 0; index < filteredItems.size(); index++) {
      final Object item = filteredItems.get(index);
      if (item instanceof Entity && ((Entity) item).getPrimaryKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private static final class ForeignKeyModelRefreshListener extends EventAdapter {

    private final EntityComboBoxModel foreignKeyModel;

    public ForeignKeyModelRefreshListener(final EntityComboBoxModel foreignKeyModel) {
      this.foreignKeyModel = foreignKeyModel;
    }

    /** {@inheritDoc} */
    @Override
    public void eventOccurred() {
      foreignKeyModel.forceRefresh();
    }
  }
}