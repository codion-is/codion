/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import org.apache.log4j.Logger;

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
public class EntityComboBoxModel extends FilteredComboBoxModel {

  private static final Logger LOG = Util.getLogger(EntityComboBoxModel.class);

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
  private final boolean staticData;

  /**
   * true after the data has been fetched for the first time
   */
  private boolean dataInitialized = false;//todo move to super class, cleared or something?

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
        if (filterValues.size() > 0 && !filterValues.contains(foreignKeyValue))
          return false;
      }

      return true;
    }
  };

  /**
   * @param entityID the ID of the entity this combo box model should represent
   * @param dbProvider a EntityDbProvider instance
   */
  public EntityComboBoxModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, false);
  }

  /**
   * @param entityID the ID of the entity this combo box model should represent
   * @param dbProvider a EntityDbProvider instance
   * @param staticData if true this combo box model is refreshed only on initialization
   * and on subsequent calls to <code>forceRefresh</code>
   */
  public EntityComboBoxModel(final String entityID, final EntityDbProvider dbProvider,
                             final boolean staticData) {
    this(entityID, dbProvider, staticData, null);
  }

  /**
   * @param entityID the ID of the entity this combo box model should represent
   * @param dbProvider a EntityDbProvider instance
   * @param staticData if true this combo box model is refreshed only on initialization
   * and on subsequent calls to <code>forceRefresh</code>
   * @param nullValueItem the item to used to represent a null value
   */
  public EntityComboBoxModel(final String entityID, final EntityDbProvider dbProvider,
                             final boolean staticData, final String nullValueItem) {
    this(entityID, dbProvider, staticData, nullValueItem, true);
  }

  /**
   * @param entityID the ID of the entity this combo box model should represent
   * @param dbProvider a EntityDbProvider instance
   * @param staticData if true this combo box model is refreshed only on initialization
   * and on subsequent calls to <code>forceRefresh</code>
   * @param nullValueItem the item to used to represent a null value
   * @param sortContents if true, the contents are sorted
   */
  public EntityComboBoxModel(final String entityID, final EntityDbProvider dbProvider,
                             final boolean staticData, final String nullValueItem, final boolean sortContents) {
    super(sortContents, nullValueItem);
    if (entityID == null)
      throw new IllegalArgumentException("EntityComboBoxModel requires a non-null entityID");
    if (dbProvider == null)
      throw new IllegalArgumentException("EntityComboBoxModel requires a non-null dbProvider");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.staticData = staticData;
  }

  /**
   * @return the EntityDbProvider instance used by this EntityComboBoxModel
   */
  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * @return the ID of the underlying entity
   */
  public String getEntityID() {
    return entityID;
  }

  /**
   * Forces a refresh of this model, disregarding the staticData directive
   */
  public void forceRefresh() {
    try {
      forceRefresh = true;
      refresh();
    }
    finally  {
      forceRefresh = false;
    }
  }

  /**
   * Clears the contents from this model
   */
  public void clear() {
    super.clear();
    dataInitialized = false;
  }

  /**
   * Selects the entity with the given primary key, if the entity is not available
   * in the model this method returns silently without changing the selection
   * @param primaryKey the primary key of the entity to select
   */
  public void setSelectedEntityByPrimaryKey(final Entity.Key primaryKey) {
    final Entity toSelect = new Entity(primaryKey);
    List<Object> items = getVisibleItems();
    int indexOfKey = items.indexOf(toSelect);
    if (indexOfKey >= 0)
      setSelectedItem(items.get(indexOfKey));
    items = getFilteredItems();
    indexOfKey = items.indexOf(toSelect);
    if (indexOfKey >= 0)
      setSelectedItem(items.get(indexOfKey));
  }

  /**
   * @return the selected entity
   */
  public Entity getSelectedEntity() {
    if (isNullValueSelected())
      return null;

    return (Entity) getSelectedItem();
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItem(final Object toSelect) {
    if (getSize() == 0)
      return;
    final Object item = toSelect instanceof String && ((String)toSelect).length() == 0 ? null : toSelect;
    if (item != null && !item.equals(getNullValueString()) && !(item instanceof Entity))
      throw new IllegalArgumentException("Cannot set '" + item + "' [" + item.getClass()
              + "] as selected item in a EntityComboBoxModel (" + this + ")");

    if (item instanceof Entity) {
      final int indexOfKey = getIndexOfKey(((Entity) toSelect).getPrimaryKey());
      if (indexOfKey >= 0)
        super.setSelectedItem(getElementAt(indexOfKey));
      else
        super.setSelectedItem(toSelect);
    }
    else {
      super.setSelectedItem(null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getClass().getSimpleName() + " [entityID: " + getEntityID() + "]";
  }

  /**
   * @return true if the data has been initialized
   */
  public boolean isDataInitialized() {
    return dataInitialized;
  }

  /**
   * Sets the criteria to use when querying data
   * @param selectCriteria the criteria
   */
  public void setEntitySelectCriteria(final EntitySelectCriteria selectCriteria) {
    if (selectCriteria != null && !selectCriteria.getEntityID().equals(getEntityID()))
      throw new RuntimeException("EntitySelectCriteria entityID mismatch, " + getEntityID()
              + " expected, got " + selectCriteria.getEntityID());
    this.selectCriteria = selectCriteria;
  }

  public void setForeignKeyFilterEntities(final String foreignKeyPropertyID, final Collection<Entity> entities) {
    final Set<Entity> filterEntities = new HashSet<Entity>();
    if (entities != null)
      filterEntities.addAll(entities);
    foreignKeyFilterEntities.put(foreignKeyPropertyID, filterEntities);

    filterContents();
  }

  public Collection<Entity> getForeignKeyFilterEntities(final String foreignKeyPropertyID) {
    final Collection<Entity> filterEntities = new ArrayList<Entity>();
    if (foreignKeyFilterEntities.containsKey(foreignKeyPropertyID))
      filterEntities.addAll(foreignKeyFilterEntities.get(foreignKeyPropertyID));

    return filterEntities;
  }

  public EntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyID) {
    final Property.ForeignKeyProperty foreignKeyProperty =
            EntityRepository.getForeignKeyProperty(getEntityID(), foreignKeyPropertyID);
    final EntityComboBoxModel foreignKeyModel = new EntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(),
            getDbProvider(), true, "-", true);
    foreignKeyModel.refresh();
    final Collection<Entity> filterEntities = getForeignKeyFilterEntities(foreignKeyPropertyID);
    if (filterEntities != null && filterEntities.size() > 0)
      foreignKeyModel.setSelectedItem(filterEntities.iterator().next());
    foreignKeyModel.eventSelectionChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final Entity selectedEntity = foreignKeyModel.getSelectedEntity();
        setForeignKeyFilterEntities(foreignKeyPropertyID,
                selectedEntity == null ? new ArrayList<Entity>(0) : Arrays.asList(selectedEntity));
      }
    });
    eventSelectionChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final Entity selected = getSelectedEntity();
        if (selected != null)
          foreignKeyModel.setSelectedEntityByPrimaryKey(selected.getReferencedPrimaryKey(foreignKeyProperty));
      }
    });

    return foreignKeyModel;
  }

  /**
   * @return an Event fired when a refresh has been performed
   */
  public Event eventRefreshDone() {
    return evtRefreshDone;
  }

  /**
   * @return the EntitySelectCriteria used by this EntityComboBoxModel
   */
  protected EntitySelectCriteria getEntitySelectCriteria() {
    return selectCriteria;
  }

  /**
   * Returns true if the given Entity should be included in this ComboBoxModel.
   * To be overridden in subclasses wishing to exclude some entities
   * @param entity the Entity object to check
   * @return true if the Entity should be included, false otherwise
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected boolean includeEntity(final Entity entity) {
    return true;
  }

  @Override
  protected boolean include(final Object object) {
    if (object instanceof Entity)
      return super.include(object) && foreignKeyFilterCriteria.include((Entity) object);
    else
      return super.include(object);
  }

  /**
   * @return the data to be presented in this EntityComboBoxModel, called when the data is refreshed
   */
  @Override
  protected List<?> getContents() {
    try {
      if (staticData && dataInitialized && !forceRefresh) {
        LOG.trace(this + " refresh not required");
        return super.getContents();
      }
      final List<Entity> entities = performQuery();
      final ListIterator<Entity> iterator = entities.listIterator();
      while (iterator.hasNext())
        if (!includeEntity(iterator.next()))
          iterator.remove();

      return entities;
    }
    finally {
      dataInitialized = true;
      evtRefreshDone.fire();
      LOG.trace(this + " done refreshing" + (forceRefresh ? " (forced)" : ""));
    }
  }

  /**
   * Retrieves the entities to present in this EntityComboBoxModel
   * @return the entities to present in this EntityComboBoxModel
   */
  protected List<Entity> performQuery() {
    try {
      if (getEntitySelectCriteria() != null)
        return dbProvider.getEntityDb().selectMany(getEntitySelectCriteria());
      else
        return dbProvider.getEntityDb().selectAll(getEntityID());
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