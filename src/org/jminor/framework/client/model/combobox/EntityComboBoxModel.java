/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.ListIterator;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table
 */
public class EntityComboBoxModel extends FilteredComboBoxModel {

  private static final Logger log = Util.getLogger(EntityComboBoxModel.class);

  /**
   * fired when a refresh has been performed
   */
  public final Event evtRefreshDone = new Event();

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
  private boolean dataInitialized = false;

  /**
   * used to indicate that a refresh is being forced
   */
  private boolean forceRefresh = false;

  /**
   * the EntityCriteria used to filter the data
   */
  private EntityCriteria entityCriteria;

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
    setContents(null);
    dataInitialized = false;
  }

  /**
   * Selects the entity with the given primary key, if the entity is not available
   * in the model this method returns silently without changing the selection
   * @param primaryKey the primary key of the entity to select
   */
  public void setSelectedEntityByPrimaryKey(final Entity.Key primaryKey) {
    final int size = getSize();
    for (int i = 0; i < size; i++) {
      final Object item = getElementAt(i);
      if (item instanceof Entity) {
        if (((Entity) item).getPrimaryKey().equals(primaryKey)) {
          super.setSelectedItem(item);
          return;
        }
      }
    }
  }

  /**
   * @return the selected entity
   */
  public Entity getSelectedEntity() {
    if (isNullValueItemSelected())
      return null;

    return (Entity) getSelectedItem();
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedItem(final Object toSelect) {
    if (getSize() == 0)
      return;
    final Object item = toSelect instanceof String && ((String)toSelect).length() == 0 ? null : toSelect;
    if (item != null && !item.equals(getNullValueItem()) && !(item instanceof Entity))
      throw new IllegalArgumentException("Cannot set '" + item + "' [" + item.getClass()
              + "] as selected item in a EntityComboBoxModel (" + this + ")");

    if (item == null || !(item instanceof Entity))
      super.setSelectedItem(null);
    else
      setSelectedEntityByPrimaryKey(((Entity)toSelect).getPrimaryKey());
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
   * @param entityCriteria the criteria
   */
  public void setEntityCriteria(final EntityCriteria entityCriteria) {
    if (entityCriteria != null && !entityCriteria.getEntityID().equals(getEntityID()))
      throw new RuntimeException("EntityCriteria entityID mismatch, " + getEntityID()
              + " expected, got " + entityCriteria.getEntityID());
    this.entityCriteria = entityCriteria;
  }

  /**
   * @return the EntityCriteria used by this EntityComboBoxModel
   */
  protected EntityCriteria getEntityCriteria() {
    return entityCriteria;
  }

  /**
   * Returns true if the given Entity should be included in this ComboBoxModel.
   * To be overridden in subclasses wishing to exclude some entities
   * @param entity the Entity object to check
   * @return true if the Entity should be included, false otherwise
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected boolean include(final Entity entity) {
    return true;
  }

  /**
   * @return the data to be presented in this EntityComboBoxModel, called when the data is refreshed
   */
  @Override
  protected List<?> getContents() {
    try {
      if (staticData && dataInitialized && !forceRefresh) {
        log.trace(this + " refresh not required");
        return super.getContents();
      }
      final List<Entity> entities = performQuery();
      final ListIterator<Entity> iterator = entities.listIterator();
      while (iterator.hasNext())
        if (!include(iterator.next()))
          iterator.remove();

      return entities;
    }
    finally {
      dataInitialized = true;
      evtRefreshDone.fire();
      log.trace(this + " done refreshing" + (forceRefresh ? " (forced)" : ""));
    }
  }

  /**
   * Retrieves the entities to present in this EntityComboBoxModel
   * @return the entities to present in this EntityComboBoxModel
   */
  protected List<Entity> performQuery() {
    try {
      if (getEntityCriteria() != null)
        return dbProvider.getEntityDb().selectMany(getEntityCriteria());
      else
        return dbProvider.getEntityDb().selectAll(getEntityID());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}