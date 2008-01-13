/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.DbException;
import org.jminor.common.model.Event;
import org.jminor.common.model.IRefreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.Entity;

import org.apache.log4j.Logger;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.List;

public class EntityListModel extends AbstractListModel implements IRefreshable {

  private static final Logger log = Util.getLogger(EntityListModel.class);

  public final Event evtRefreshDone = new Event("EntityComboBoxModel.evtRefreshDone");

  public final State stSelectionEmpty = new State();

  public final Event evtSelectionChangedAdjusting = new Event("EntityListModel.evtSelectionChangedAdjusting");
  public final Event evtSelectionChanged = new Event("EntityListModel.evtSelectionChanged");

  private final IEntityDbProvider dbProvider;
  private final String entityID;
  private final List<Entity> data = new ArrayList<Entity>();
  private final boolean staticData;

  private boolean dataInitialized = false;

  private final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel() {
    public void fireValueChanged(int min, int max, boolean isAdjusting) {
      super.fireValueChanged(min, max, isAdjusting);
      stSelectionEmpty.setActive(isSelectionEmpty());
      if (isAdjusting)
        evtSelectionChangedAdjusting.fire();
      else
        evtSelectionChanged.fire();
    }
  };

  public EntityListModel(final IEntityDbProvider dbProvider, final String entityID) {
    this(dbProvider, entityID, false);
  }

  public EntityListModel(final IEntityDbProvider dbProvider, final String entityID,
                         final boolean staticData) {
    this.dbProvider = dbProvider;
    this.entityID = entityID;
    this.staticData = staticData;
  }

  /**
   * @return Value for property 'selectionModel'.
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
      final List<Entity> entities = getEntitiesFromDb();

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
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void clear() {
    data.clear();
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

  protected List<Entity> getEntitiesFromDb() throws UserException, DbException {
    try {
      return dbProvider.getEntityDb().selectAll(entityID, true);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
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
}
