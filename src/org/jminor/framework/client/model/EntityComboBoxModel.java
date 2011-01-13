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
 * Specifies a ComboBoxModel containing entities.
 */
public interface EntityComboBoxModel extends FilteredComboBoxModel<Entity>, EntityDataProvider {

  /**
   * @param primaryKey the primary key of the entity to fetch from this model
   * @return the entity with the given key if it is contained in the model, null otherwise
   */
  Entity getEntity(final Entity.Key primaryKey);

  /**
   * @param foreignKeyPropertyID the ID of the foreign key property
   * @return the entity values currently used to filter the contents of this model
   */
  Collection<Entity> getForeignKeyFilterEntities(final String foreignKeyPropertyID);

  /**
   * Filters this combo box model so that only entities referencing the given foreign key entities
   * via the given foreign key property are shown.
   * @param foreignKeyPropertyID the property ID
   * @param entities the entities
   */
  void setForeignKeyFilterEntities(final String foreignKeyPropertyID, final Collection<Entity> entities);

  /**
   * Returns a combo box model for selecting a foreign key value for filtering this model
   * @param foreignKeyPropertyID the ID of the property with which values to create the combo box model
   * @return a combo box model for selecting a filtering value for this combo box model
   */
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
