/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.DbException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityDbProvider;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.domain.Entity;

import org.apache.log4j.Logger;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.List;

public class EntityListModel extends AbstractListModel implements Refreshable {

  private static final Logger log = Util.getLogger(EntityListModel.class);

  public final Event evtRefreshDone = new Event();

  public final State stSelectionEmpty = new State();

  public final Event evtSelectionChangedAdjusting = new Event();
  public final Event evtSelectionChanged = new Event();

  private final String entityID;
  private final EntityDbProvider dbProvider;
  private final List<Entity> data = new ArrayList<Entity>();
  private final boolean staticData;

  /**
   * the EntityCriteria used to filter the data
   */
  private EntityCriteria entityCriteria;

  private boolean dataInitialized = false;

  private final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel() {
    @Override
    public void fireValueChanged(int min, int max, boolean isAdjusting) {
      super.fireValueChanged(min, max, isAdjusting);
      stSelectionEmpty.setActive(isSelectionEmpty());
      if (isAdjusting)
        evtSelectionChangedAdjusting.fire();
      else
        evtSelectionChanged.fire();
    }
  };

  public EntityListModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, false);
  }

  public EntityListModel(final String entityID, final EntityDbProvider dbProvider, final boolean staticData) {
    if (entityID == null)
      throw new IllegalArgumentException("EntityListModel requires a non-null entityID");
    if (dbProvider == null)
      throw new IllegalArgumentException("EntityListModel requires a non-null dbProvider");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.staticData = staticData;
  }

  /**
   * @return the list selection model
   */
  public ListSelectionModel getSelectionModel() {
    return selectionModel;
  }

  /**
   * Refreshes this model
   * @see #evtRefreshDone
   */
  public void refresh() {
    try {
      if ((staticData && dataInitialized))
        return;

      log.trace(this + " refreshing");
      data.clear();
      final List<Entity> entities = performQuery();

      for (final Entity entity : entities) {
        if (include(entity))
          data.add(entity);
      }

      dataInitialized = true;
      evtRefreshDone.fire();
    }
    catch (UserException ue) {
      throw ue.getRuntimeException();
    }
    catch (DbException e) {
      throw new RuntimeException(e);
    }
  }

  public void clear() {
    data.clear();
  }

  public String getEntityID() {
    return entityID;
  }

  public Entity getEntityAt(final int index) {
    return (Entity) getElementAt(index);
  }

  /** {@inheritDoc} */
  public Object getElementAt(final int index) {
    return data.get(index);
  }

  /** {@inheritDoc} */
  public int getSize() {
    return data.size();
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
   * Returns true if the given Entity should be included in this ListModel.
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
  protected List<Entity> performQuery() throws UserException, DbException {
    try {
      if (getEntityCriteria() != null)
        return dbProvider.getEntityDb().selectMany(getEntityCriteria());
      else
        return dbProvider.getEntityDb().selectAll(getEntityID());
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }
}
