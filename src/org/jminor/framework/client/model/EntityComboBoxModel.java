/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.FilterCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entity;

import javax.swing.ComboBoxModel;
import java.util.Collection;

/**
 * User: darri
 * Date: 29.6.2010
 * Time: 10:10:25
 */
public interface EntityComboBoxModel extends ComboBoxModel, Refreshable, FilterCriteria<Object> {

  /**
   * @return the selected entity or null if none is selected
   */
  Entity getSelectedEntity();

  /**
   * @return an Event fired when a refresh has been performed
   */
  Event eventRefreshDone();

  /**
   * @return the ID of the underlying entity
   */
  String getEntityID();

  /**
   * @return the EntityDbProvider instance used by this EntityComboBoxModel
   */
  EntityDbProvider getDbProvider();

  Collection<Entity> getForeignKeyFilterEntities(final String foreignKeyPropertyID);

  void setForeignKeyFilterEntities(final String foreignKeyPropertyID, final Collection<Entity> entities);

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
  public EntityComboBoxModel setStaticData(final boolean staticData);

  /**
   * @return true if the data for this model should only be fetched once
   * @see #forceRefresh()
   */
  boolean isStaticData();

  /**
   * @return true if the underlying data has been fetched and not cleared since
   */
  boolean isDataInitialized();

  /**
   * Sets the criteria to use when querying data
   * @param entitySelectCriteria the criteria
   */
  void setEntitySelectCriteria(final EntitySelectCriteria entitySelectCriteria);

  EntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyID);

  //belongs in FilteredComboBoxModel
  Event eventSelectionChanged();

  /**
   * Sets the nullValueItem, a refresh is required for it to show up
   * @param nullValueString a String representing a null value
   */
  void setNullValueString(final String nullValueString);

  /**
   * Returns true if the given item is visible in this combo box model,
   * null values are considered visible if a <code>nullValueString</code>
   * has been specified.
   * @param item the item
   * @return true if the given item is visible
   */
  boolean isVisible(final Object item);

  /**
   * Removes the given item from this model
   * @param item the item to remove
   */
  void removeItem(final Object item);

  /**
   * Filters the contents of this model according to the <code>include</code> method
   * @see #include(Object)
   */
  void filterContents();

  /**
   * @param filterCriteria the FilterCriteria to use
   */
  void setFilterCriteria(final FilterCriteria filterCriteria);
}
