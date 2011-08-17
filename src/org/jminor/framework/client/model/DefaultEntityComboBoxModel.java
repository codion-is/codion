/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Event;
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
   * the EntitySelectCriteria used to filter the data
   */
  private EntitySelectCriteria selectCriteria;

  /**
   * A map of entities used to filter the contents of this model
   */
  private final Map<String, Set<Entity>> foreignKeyFilterEntities = new HashMap<String, Set<Entity>>();

  private final FilterCriteria<Entity> foreignKeyFilterCriteria = new FilterCriteria<Entity>() {
    /** {@inheritDoc} */
    public boolean include(final Entity item) {
      for (final Map.Entry<String, Set<Entity>> entry : foreignKeyFilterEntities.entrySet()) {
        final Entity foreignKeyValue = item.getForeignKeyValue(entry.getKey());
        final Set<Entity> filterValues = entry.getValue();
        if (!filterValues.isEmpty() && !filterValues.contains(foreignKeyValue)) {
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
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
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
  public final boolean isStaticData() {
    return staticData;
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel setStaticData(final boolean staticData) {
    this.staticData = staticData;
    return this;
  }

  /** {@inheritDoc} */
  public final Entity getEntity(final Entity.Key primaryKey) {
    for (final Entity entity : getAllItems()) {
      if (entity.getPrimaryKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  public final void setSelectedEntityByPrimaryKey(final Entity.Key primaryKey) {
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
  public final void setForeignKeyFilterEntities(final String foreignKeyPropertyID, final Collection<Entity> entities) {
    final Set<Entity> filterEntities = new HashSet<Entity>();
    if (entities != null) {
      filterEntities.addAll(entities);
    }
    foreignKeyFilterEntities.put(foreignKeyPropertyID, filterEntities);

    filterContents();
  }

  /** {@inheritDoc} */
  public final Collection<Entity> getForeignKeyFilterEntities(final String foreignKeyPropertyID) {
    final Collection<Entity> filterEntities = new ArrayList<Entity>();
    if (foreignKeyFilterEntities.containsKey(foreignKeyPropertyID)) {
      filterEntities.addAll(foreignKeyFilterEntities.get(foreignKeyPropertyID));
    }

    return filterEntities;
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyID) {
    final Property.ForeignKeyProperty foreignKeyProperty = Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID);
    final EntityComboBoxModel foreignKeyModel =
            new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider);
    foreignKeyModel.setNullValueString("-");
    foreignKeyModel.refresh();
    linkForeignKeyComboBoxModel(foreignKeyPropertyID, this, foreignKeyModel);

    return foreignKeyModel;
  }

  /** {@inheritDoc} */
  public final void addRefreshListener(final ActionListener listener) {
    evtRefreshDone.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeRefreshListener(final ActionListener listener) {
    evtRefreshDone.removeListener(listener);
  }

  //todo move somewhere else?
  public static void linkForeignKeyComboBoxModel(final String foreignKeyPropertyID, final EntityComboBoxModel model, final EntityComboBoxModel foreignKeyModel) {
    final Collection<Entity> filterEntities = model.getForeignKeyFilterEntities(foreignKeyPropertyID);
    if (filterEntities != null && !filterEntities.isEmpty()) {
      foreignKeyModel.setSelectedItem(filterEntities.iterator().next());
    }
    foreignKeyModel.addSelectionListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        final Entity selectedEntity = foreignKeyModel.getSelectedValue();
        model.setForeignKeyFilterEntities(foreignKeyPropertyID,
                selectedEntity == null ? new ArrayList<Entity>(0) : Arrays.asList(selectedEntity));
      }
    });
    model.addSelectionListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        final Entity selected = model.getSelectedValue();
        if (selected != null) {
          foreignKeyModel.setSelectedEntityByPrimaryKey(selected.getReferencedPrimaryKey(
                  Entities.getForeignKeyProperty(model.getEntityID(), foreignKeyPropertyID)));
        }
      }
    });
    model.addRefreshListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        foreignKeyModel.forceRefresh();
      }
    });
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
    if (getSize() == 0) {
      return true;
    }
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
      final Object item = filteredItems.get(0);
      if (item instanceof Entity && ((Entity) item).getPrimaryKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }
}