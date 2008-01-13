/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.combobox;

import org.jminor.common.db.DbException;
import org.jminor.common.db.TableStatus;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityKey;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.ListIterator;

public class EntityComboBoxModel extends FilteredComboBoxModel {

  private static final Logger log = Util.getLogger(EntityComboBoxModel.class);

  public final Event evtRefreshDone = new Event("EntityComboBoxModel.evtRefreshDone");

  private boolean forceRefresh = false;
  private final TableStatus tableStatus = new TableStatus();

  private final String entityID;
  private final IEntityDbProvider dbProvider;

  //if true prevents refreshing from db
  private boolean staticData = false;
  private boolean dataInitialized = false;

  public EntityComboBoxModel(final IEntityDbProvider dbProvider, final String entityID) {
    this(dbProvider, entityID, false);
  }

  public EntityComboBoxModel(final IEntityDbProvider dbProvider, final String entityID,
                             final boolean staticData) {
    this(dbProvider, entityID, staticData, null);
  }

  public EntityComboBoxModel(final IEntityDbProvider dbProvider, final String entityID,
                             final boolean staticData, final String firstItem) {
    this(dbProvider, entityID, staticData, firstItem, false);
  }

  public EntityComboBoxModel(final IEntityDbProvider dbProvider, final String entityID,
                             final boolean staticData, final String firstItem, final boolean sortContents) {
    super(sortContents, firstItem);
    this.staticData = staticData;
    this.dbProvider = dbProvider;
    this.entityID = entityID;
  }

  /**
   * @return Value for property 'dbProvider'.
   */
  public IEntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * @return Value for property 'entityID'.
   */
  public String getEntityID() {
    return entityID;
  }

  /**
   * @param value Value to set for property 'forceRefresh'.
   */
  public void setForceRefresh(boolean value) {
    forceRefresh = value;
  }

  /**
   * Forces a refresh of this model, disregarding the status of the underlying table
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  public void forceRefresh() throws UserException {
    try {
      setForceRefresh(true);
      refresh();
    }
    finally  {
      setForceRefresh(false);
    }
  }

  protected List<?> getContents() {
    try {
      if ((staticData && dataInitialized) || (FrameworkSettings.get().useSmartRefresh && !isRefreshRequired())) {
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
    tableStatus.setNull();
    dataInitialized = false;
  }

  /**
   * @param primaryKey Value to set for property 'selectedEntityByPrimaryKey'.
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
   * @return Value for property 'selectedEntity'.
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
    return getEntityID();
  }

  public boolean isDataInitialized() {
    return dataInitialized;
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

  protected List<Entity> getEntitiesFromDb() throws UserException, DbException {
    try {
      return dbProvider.getEntityDb().selectAll(getEntityID(), true);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  /**
   * @return Value for property 'refreshRequired'.
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  protected boolean isRefreshRequired() throws UserException {
    if (!tableStatus.isNull() && !tableStatus.tableHasAuditColumns())
      return true;

    final TableStatus currentTableStatus;
    try {
      currentTableStatus = dbProvider.getEntityDb().getTableStatus(getEntityID(), tableStatus.tableHasAuditColumns());
    }
    catch (UserException ue) {
      throw ue;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
    tableStatus.setTableHasAuditColumns(currentTableStatus.tableHasAuditColumns());
    if (forceRefresh || currentTableStatus.isNull() || !currentTableStatus.equals(tableStatus)) {
      tableStatus.setLastChange(currentTableStatus.getLastChange());
      tableStatus.setRecordCount(currentTableStatus.getRecordCount());
      return true;
    }

    return false;
  }
}