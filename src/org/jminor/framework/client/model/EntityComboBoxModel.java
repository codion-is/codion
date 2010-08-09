/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entity;

import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * User: darri
 * Date: 29.6.2010
 * Time: 10:10:25
 */
public interface EntityComboBoxModel extends FilteredComboBoxModel<Entity>, EntityDataProvider {

  /**
   * @return the selected entity or null if none is selected
   */
  Entity getSelectedEntity();

  Collection<Entity> getForeignKeyFilterEntities(final String foreignKeyPropertyID);

  void setForeignKeyFilterEntities(final String foreignKeyPropertyID, final Collection<Entity> entities);

  EntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyID);

  /**
   * Selects the entity with the given primary key, if the entity is not available
   * in the model this method returns silently without changing the selection
   * @param primaryKey the primary key of the entity to select
   */
  void setSelectedEntityByPrimaryKey(final Entity.Key primaryKey);

  /**
   * Forces a refresh of this model, disregarding the staticData directive
   */
  void forceRefresh();

  /**
   * Specifies whether or not this model's data should be considered static,
   * that is, only fetched once.
   * @param staticData the value
   * @return this EntityComboBoxModel instance
   */
  EntityComboBoxModel setStaticData(final boolean staticData);

  /**
   * @return true if the data for this model should only be fetched once
   * @see #forceRefresh()
   */
  boolean isStaticData();

  /**
   * Sets the criteria to use when querying data
   * @param entitySelectCriteria the criteria
   */
  void setEntitySelectCriteria(final EntitySelectCriteria entitySelectCriteria);

  /**
   * @param listener a listener to be notified each time this model is refreshed
   */
  void addRefreshListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshListener(final ActionListener listener);
}
