/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.DefaultFilteredComboBoxModel;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 */
public class DefaultEntityComboBoxModel extends DefaultFilteredComboBoxModel<Entity> implements EntityComboBoxModel {

  private final Event evtRefreshDone = new Event();

  /**
   * the ID of the underlying entity
   */
  private final String entityID;

  /**
   * the EntityDbProvider instance used by this EntityComboBoxModel
   */
  private final EntityDbProvider dbProvider;

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
  private Map<String, Set<Entity>> foreignKeyFilterEntities = new HashMap<String, Set<Entity>>();

  private final FilterCriteria<Entity> foreignKeyFilterCriteria = new FilterCriteria<Entity>() {
    public boolean include(final Entity item) {
      for (final Map.Entry<String, Set<Entity>> entry : foreignKeyFilterEntities.entrySet()) {
        final Entity foreignKeyValue = item.getForeignKeyValue(entry.getKey());
        final Set<Entity> filterValues = entry.getValue();
        if (filterValues.size() > 0 && !filterValues.contains(foreignKeyValue)) {
          return false;
        }
      }

      return true;
    }
  };

  /**
   * @param entityID the ID of the entity this combo box model should represent
   * @param dbProvider a EntityDbProvider instance
   */
  public DefaultEntityComboBoxModel(final String entityID, final EntityDbProvider dbProvider) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(dbProvider, "dbProvider");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
  }

  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public String getEntityID() {
    return entityID;
  }

  public void forceRefresh() {
    try {
      forceRefresh = true;
      refresh();
    }
    finally  {
      forceRefresh = false;
    }
  }

  public boolean isStaticData() {
    return staticData;
  }

  public EntityComboBoxModel setStaticData(boolean staticData) {
    this.staticData = staticData;
    return this;
  }

  public void setSelectedEntityByPrimaryKey(final Entity.Key primaryKey) {
    final Entity toSelect = new Entity(primaryKey);
    List<Entity> items = getVisibleItems();
    int indexOfKey = items.indexOf(toSelect);
    if (indexOfKey >= 0) {
      setSelectedItem(items.get(indexOfKey));
    }
    items = getFilteredItems();
    indexOfKey = items.indexOf(toSelect);
    if (indexOfKey >= 0) {
      setSelectedItem(items.get(indexOfKey));
    }
  }

  public Entity getSelectedEntity() {
    if (isNullValueSelected()) {
      return null;
    }

    return (Entity) getSelectedItem();
  }

  @Override
  public void setSelectedItem(final Object anItem) {
    if (getSize() == 0) {
      return;
    }
    final Object item = anItem instanceof String && ((String) anItem).length() == 0 ? null : anItem;
    if (item != null && !item.equals(getNullValueString()) && !(item instanceof Entity)) {
      throw new IllegalArgumentException("Cannot set '" + item + "' [" + item.getClass()
              + "] as selected item in a EntityComboBoxModel (" + this + ")");
    }

    if (item instanceof Entity) {
      final int indexOfKey = getIndexOfKey(((Entity) anItem).getPrimaryKey());
      if (indexOfKey >= 0) {
        super.setSelectedItem(getElementAt(indexOfKey));
      }
      else {
        super.setSelectedItem(anItem);
      }
    }
    else {
      super.setSelectedItem(null);
    }
  }

  @Override
  public FilterCriteria<Entity> getFilterCriteria() {
    final FilterCriteria superCriteria = super.getFilterCriteria();
    return new FilterCriteria<Entity>() {
      public boolean include(final Entity item) {
        if (!superCriteria.include(item)) {
          return false;
        }

        return foreignKeyFilterCriteria.include(item);
      }
    };
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [entityID: " + entityID + "]";
  }

  public void setEntitySelectCriteria(final EntitySelectCriteria entitySelectCriteria) {
    if (entitySelectCriteria != null && !entitySelectCriteria.getEntityID().equals(entityID)) {
      throw new RuntimeException("EntitySelectCriteria entityID mismatch, " + entityID
              + " expected, got " + entitySelectCriteria.getEntityID());
    }
    this.selectCriteria = entitySelectCriteria;
  }

  public void setForeignKeyFilterEntities(final String foreignKeyPropertyID, final Collection<Entity> entities) {
    final Set<Entity> filterEntities = new HashSet<Entity>();
    if (entities != null) {
      filterEntities.addAll(entities);
    }
    foreignKeyFilterEntities.put(foreignKeyPropertyID, filterEntities);

    filterContents();
  }

  public Collection<Entity> getForeignKeyFilterEntities(final String foreignKeyPropertyID) {
    final Collection<Entity> filterEntities = new ArrayList<Entity>();
    if (foreignKeyFilterEntities.containsKey(foreignKeyPropertyID)) {
      filterEntities.addAll(foreignKeyFilterEntities.get(foreignKeyPropertyID));
    }

    return filterEntities;
  }

  public EntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyID) {
    final Property.ForeignKeyProperty foreignKeyProperty =
            EntityRepository.getForeignKeyProperty(entityID, foreignKeyPropertyID);
    final EntityComboBoxModel foreignKeyModel
            = new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), dbProvider);
    foreignKeyModel.setNullValueString("-");
    foreignKeyModel.refresh();
    linkForeignKeyComboBoxModel(foreignKeyPropertyID, this, foreignKeyModel);

    return foreignKeyModel;
  }

  public Event eventRefreshDone() {
    return evtRefreshDone;
  }

  //todo move somewhere else?
  public static void linkForeignKeyComboBoxModel(final String foreignKeyPropertyID, final EntityComboBoxModel model, final EntityComboBoxModel foreignKeyModel) {
    final Collection<Entity> filterEntities = model.getForeignKeyFilterEntities(foreignKeyPropertyID);
    if (filterEntities != null && filterEntities.size() > 0) {
      foreignKeyModel.setSelectedItem(filterEntities.iterator().next());
    }
    foreignKeyModel.eventSelectionChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final Entity selectedEntity = foreignKeyModel.getSelectedEntity();
        model.setForeignKeyFilterEntities(foreignKeyPropertyID,
                selectedEntity == null ? new ArrayList<Entity>(0) : Arrays.asList(selectedEntity));
      }
    });
    model.eventSelectionChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final Entity selected = model.getSelectedEntity();
        if (selected != null) {
          foreignKeyModel.setSelectedEntityByPrimaryKey(selected.getReferencedPrimaryKey(
                  EntityRepository.getForeignKeyProperty(model.getEntityID(), foreignKeyPropertyID)));
        }
      }
    });
    model.eventRefreshDone().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        foreignKeyModel.forceRefresh();
      }
    });
  }

  /**
   * @return the EntitySelectCriteria used by this EntityComboBoxModel
   */
  protected EntitySelectCriteria getEntitySelectCriteria() {
    return selectCriteria;
  }

  /**
   * @return the data to be presented in this EntityComboBoxModel, called when the data is refreshed
   */
  @Override
  protected List<Entity> initializeContents() {
    try {
      if (staticData && !isCleared() && !forceRefresh) {
        return super.initializeContents();
      }
      final List<Entity> entities = performQuery();
      final ListIterator<Entity> iterator = entities.listIterator();
      while (iterator.hasNext()) {
        if (!getFilterCriteria().include(iterator.next())) {
          iterator.remove();
        }
      }

      return entities;
    }
    finally {
      evtRefreshDone.fire();
    }
  }

  /**
   * Retrieves the entities to present in this EntityComboBoxModel
   * @return the entities to present in this EntityComboBoxModel
   */
  protected List<Entity> performQuery() {
    try {
      if (selectCriteria != null) {
        return dbProvider.getEntityDb().selectMany(selectCriteria);
      }
      else {
        return dbProvider.getEntityDb().selectAll(entityID);
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected int getIndexOfKey(final Entity.Key primaryKey) {
    final int size = getSize();
    for (int index = 0; index < size; index++) {
      final Object item = getElementAt(index);
      if (item instanceof Entity && ((Entity) item).getPrimaryKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }
}