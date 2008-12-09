/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.combobox;

import org.jminor.common.db.DbException;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityCriteria;
import org.jminor.framework.model.EntityKey;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.ListIterator;

/**
 * A ComboBoxModel based on an Entity
 */
public class EntityComboBoxModel extends FilteredComboBoxModel {

  private static final Logger log = Util.getLogger(EntityComboBoxModel.class);

  /**
   * fired when after a refresh has been performed
   */
  public final Event evtRefreshDone = new Event("EntityComboBoxModel.evtRefreshDone");

  /**
   * the ID of the underlying entity
   */
  private final String entityID;

  /**
   * the IEntityDbProvider instance used by this EntityComboBoxModel
   */
  private final IEntityDbProvider dbProvider;

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
   * @param dbProvider a IEntityDbProvider instance
   * @param entityID the ID of the entity this combo box model should represent
   */
  public EntityComboBoxModel(final IEntityDbProvider dbProvider, final String entityID) {
    this(dbProvider, entityID, false);
  }

  /**
   * @param dbProvider a IEntityDbProvider instance
   * @param entityID the ID of the entity this combo box model should represent
   * @param staticData if true this combo box model is refreshed only on initialization
   */
  public EntityComboBoxModel(final IEntityDbProvider dbProvider, final String entityID,
                             final boolean staticData) {
    this(dbProvider, entityID, staticData, null);
  }

  /**
   * @param dbProvider a IEntityDbProvider instance
   * @param entityID the ID of the entity this combo box model should represent
   * @param staticData if true this combo box model is refreshed only on initialization
   * @param nullValueItem the item to used to represent a null value
   */
  public EntityComboBoxModel(final IEntityDbProvider dbProvider, final String entityID,
                             final boolean staticData, final String nullValueItem) {
    this(dbProvider, entityID, staticData, nullValueItem, true);
  }

  /**
   * @param dbProvider a IEntityDbProvider instance
   * @param entityID the ID of the entity this combo box model should represent
   * @param staticData if true this combo box model is refreshed only on initialization
   * @param nullValueItem the item to used to represent a null value
   * @param sortContents if true, the contents are sorted
   */
  public EntityComboBoxModel(final IEntityDbProvider dbProvider, final String entityID,
                             final boolean staticData, final String nullValueItem, final boolean sortContents) {
    super(sortContents, nullValueItem);
    this.staticData = staticData;
    this.dbProvider = dbProvider;
    this.entityID = entityID;
  }

  /**
   * @return the IEntityDbProvider instance used by this EntityComboBoxModel
   */
  public IEntityDbProvider getDbProvider() {
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
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  public void forceRefresh() throws UserException {
    try {
      forceRefresh = true;
      refresh();
    }
    finally  {
      forceRefresh = false;
    }
  }

  /**
   * @return the data to be presented in this EntityComboBoxModel, called when the data is refreshed
   */
  protected List<?> getContents() {
    try {
      if (staticData && dataInitialized && !forceRefresh) {
        log.trace(this + " refresh not required");
        return super.getContents();
      }
      final List<Entity> entities = getEntitiesFromDb();
      final ListIterator<Entity> iterator = entities.listIterator();
      while (iterator.hasNext())
        if (!include(iterator.next()))
          iterator.remove();

      return entities;
    }
    catch (UserException e) {
      throw e.getRuntimeException();
    }
    catch (DbException e) {
      throw new RuntimeException(e);
    }
    finally {
      dataInitialized = true;
      evtRefreshDone.fire();
      log.trace(this + " done refreshing" + (forceRefresh ? " (forced)" : ""));
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
   * Selects the entity with the given primary key
   * @param primaryKey the primary key of the entity to select
   */
  public void setSelectedEntityByPrimaryKey(final EntityKey primaryKey) {
    final int size = getSize();
    for (int i = 0; i < size; i++) {
      final Object item = getElementAt(i);
      if (item instanceof Entity) {
        if (((Entity) item).getPrimaryKey().equals(primaryKey)) {
          setSelectedItem(item);
          return;
        }
      }
    }
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
  public void setSelectedItem(final Object item) {
    if (getSize() == 0)
      return;
    if (item != null && item != getNullValueItem() && !(item instanceof Entity))
      throw new IllegalArgumentException("Cannot set '" + item + "' [" + item.getClass()
              + "] as selected item in a EntityComboBoxModel (" + this + ")");

    super.setSelectedItem((item instanceof Entity && ((Entity) item).isNull()) ? null : item);
  }

  /** {@inheritDoc} */
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
   * Retrieves the entities to present in this EntityComboBoxModel
   * @return the entities to present in this EntityComboBoxModel
   * @throws UserException in case of an exception
   * @throws DbException in case of a database exception
   */
  protected List<Entity> getEntitiesFromDb() throws UserException, DbException {
    try {
      if (getEntityCriteria() != null)
        return dbProvider.getEntityDb().selectMany(getEntityCriteria());
      else
        return dbProvider.getEntityDb().selectAll(getEntityID(), true);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }
}