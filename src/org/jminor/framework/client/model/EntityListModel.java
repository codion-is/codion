/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;

import org.apache.log4j.Logger;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.List;

//todo incomplete, no selection model
/**
 * A ListModel for displaying entities.
 */
public class EntityListModel extends AbstractListModel implements Refreshable {

  private static final Logger LOG = Util.getLogger(EntityListModel.class);

  private final State stSelectionEmpty = new State();
  private final Event evtRefreshDone = new Event();
  private final Event evtSelectionChangedAdjusting = new Event();
  private final Event evtSelectionChanged = new Event();

  private final String entityID;
  private final EntityDbProvider dbProvider;
  private final List<Entity> data = new ArrayList<Entity>();

  /**
   * the EntitySelectCriteria used to filter the data
   */
  private EntitySelectCriteria selectCriteria;

  private boolean staticData = false;
  private boolean dataInitialized = false;

  private final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel() {
    @Override
    public void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
      super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
      stSelectionEmpty.setActive(isSelectionEmpty());
      if (isAdjusting) {
        evtSelectionChangedAdjusting.fire();
      }
      else {
        evtSelectionChanged.fire();
      }
    }
  };

  public EntityListModel(final String entityID, final EntityDbProvider dbProvider) {
    Util.rejectNullValue(entityID);
    Util.rejectNullValue(dbProvider);
    this.entityID = entityID;
    this.dbProvider = dbProvider;
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
    if ((staticData && dataInitialized)) {
      return;
    }

    LOG.trace(this + " refreshing");
    data.clear();
    final List<Entity> entities = performQuery();

    for (final Entity entity : entities) {
      if (include(entity)) {
        data.add(entity);
      }
    }

    dataInitialized = true;
    evtRefreshDone.fire();
  }

  public boolean isStaticData() {
    return staticData;
  }

  public EntityListModel setStaticData(final boolean staticData) {
    this.staticData = staticData;
    return this;
  }

  public void clear() {
    data.clear();
  }

  public String getEntityID() {
    return entityID;
  }

  public List<Entity> getSelectedEntities() {
    final List<Entity> ret = new ArrayList<Entity>();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    for (int index = min; index <= max; index++) {
      if (selectionModel.isSelectedIndex(index)) {
        ret.add(getEntityAt(index));
      }
    }

    return ret;
  }

  public List<Entity> getAllEntities() {
    return new ArrayList<Entity>(data);
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
   * @param selectCriteria the criteria
   */
  public void setEntitySelectCriteria(final EntitySelectCriteria selectCriteria) {
    this.selectCriteria = selectCriteria;
  }

  /**
   * @return the EntitySelectCriteria used by this EntityComboBoxModel
   */
  protected EntitySelectCriteria getEntitySelectCriteria() {
    return selectCriteria;
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
}
