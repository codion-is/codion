/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.EventListener;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;

/**
 * A ComboBoxModel based on {@link Entity} instances.
 */
public interface EntityComboBoxModel extends FilteredComboBoxModel<Entity>, EntityDataProvider {

  /**
   * @param primaryKey the primary key of the entity to fetch from this model
   * @return the entity with the given key if found in the model, null otherwise
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
   * Specifies whether foreign key filtering should be strict or not.
   * When the filtering is strict only entities with the correct reference are included, that is,
   * entities with null values for the given foreign key are filtered.
   * Non-strict simply means that entities with null references are not filtered.
   * @param strictForeignKeyFiltering the value
   * @see #setForeignKeyFilterEntities(String, Collection)
   */
  void setStrictForeignKeyFiltering(final boolean strictForeignKeyFiltering);

  /**
   * @return true if strict foreign key filtering is enabled
   */
  boolean isStrictForeignKeyFiltering();

  /**
   * Returns a combo box model for selecting a foreign key value for filtering this model
   * @param foreignKeyPropertyID the ID of the property with which values to create the combo box model
   * @return a combo box model for selecting a filtering value for this combo box model
   * @see #linkForeignKeyComboBoxModel(String, EntityComboBoxModel)
   */
  EntityComboBoxModel createForeignKeyFilterComboBoxModel(final String foreignKeyPropertyID);

  /**
   * Links the given combo box model representing master entities to this combo box model
   * so that selection in the master model filters this model according to the selected master entity
   * @param foreignKeyPropertyID the ID of the foreign key property
   * @param foreignKeyModel the combo box model to link
   */
  void linkForeignKeyComboBoxModel(final String foreignKeyPropertyID, final EntityComboBoxModel foreignKeyModel);

  /**
   * Selects the entity with the given primary key, if the entity is not available
   * in the model this method returns silently without changing the selection
   * @param primaryKey the primary key of the entity to select
   */
  void setSelectedEntityByKey(final Entity.Key primaryKey);

  /**
   * Forces a refresh of this model, disregarding the staticData directive
   * @see #setStaticData(boolean)
   */
  void forceRefresh();

  /**
   * Specifies whether or not this models data should be considered static, that is, only fetched once.
   * Note that {@link #forceRefresh()} disregards this directive.
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
   * Sets the condition provider to use when querying data
   * @param selectConditionProvider the condition provider
   */
  void setSelectConditionProvider(final Condition.Provider<Property.ColumnProperty> selectConditionProvider);

  /**
   * @return the select condition provider, null if none is specified
   */
  Condition.Provider<Property.ColumnProperty> getSelectConditionProvider();

  /**
   * @param listener a listener to be notified each time this model is refreshed
   */
  void addRefreshListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshListener(final EventListener listener);
}
