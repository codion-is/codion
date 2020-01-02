/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.event.EventListener;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.domain.Entity;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * A ComboBoxModel based on {@link Entity} instances.
 */
public interface EntityComboBoxModel extends FilteredComboBoxModel<Entity> {

  /**
   * @return the ID of the entity this combo box model is based on
   */
  String getEntityId();

  /**
   * @return the connection provider used by this combo box model
   */
  EntityConnectionProvider getConnectionProvider();

  /**
   * @param primaryKey the primary key of the entity to fetch from this model
   * @return the entity with the given key if found in the model, null otherwise
   */
  Entity getEntity(Entity.Key primaryKey);

  /**
   * Use this method to retrieve the default foreign key filter condition model if you
   * want to add a custom {@link Predicate} to this model via {@link #setIncludeCondition(Predicate)}.
   * <pre>
   *   Predicate fkCondition = model.getForeignKeyIncludeCondition();
   *   model.setIncludeCondition(new Predicate() {
   *     public boolean test(Entity item) {
   *       return fkCondition.test(item) &amp;&amp; ...;
   *     }
   *   });
   * </pre>
   * @return the {@link Predicate} based on the foreign key filter entities
   * @see #setForeignKeyFilterEntities(String, Collection)
   */
  Predicate<Entity> getForeignKeyIncludeCondition();

  /**
   * @param foreignKeyPropertyId the ID of the foreign key property
   * @return the entity values currently used to filter the contents of this model
   */
  Collection<Entity> getForeignKeyFilterEntities(String foreignKeyPropertyId);

  /**
   * Filters this combo box model so that only entities referencing the given foreign key entities
   * via the given foreign key property are shown.
   * @param foreignKeyPropertyId the property ID
   * @param entities the entities
   */
  void setForeignKeyFilterEntities(String foreignKeyPropertyId, Collection<Entity> entities);

  /**
   * Specifies whether foreign key filtering should be strict or not.
   * When the filtering is strict only entities with the correct reference are included, that is,
   * entities with null values for the given foreign key are filtered.
   * Non-strict simply means that entities with null references are not filtered.
   * @param strictForeignKeyFiltering the value
   * @see #setForeignKeyFilterEntities(String, Collection)
   */
  void setStrictForeignKeyFiltering(boolean strictForeignKeyFiltering);

  /**
   * @return true if strict foreign key filtering is enabled
   */
  boolean isStrictForeignKeyFiltering();

  /**
   * Returns a combo box model for selecting a foreign key value for filtering this model
   * @param foreignKeyPropertyId the ID of the property with which values to create the combo box model
   * @return a combo box model for selecting a filtering value for this combo box model
   * @see #linkForeignKeyComboBoxModel(String, EntityComboBoxModel)
   */
  EntityComboBoxModel createForeignKeyFilterComboBoxModel(String foreignKeyPropertyId);

  /**
   * Links the given combo box model representing master entities to this combo box model
   * so that selection in the master model filters this model according to the selected master entity
   * @param foreignKeyPropertyId the ID of the foreign key property
   * @param foreignKeyModel the combo box model to link
   */
  void linkForeignKeyComboBoxModel(String foreignKeyPropertyId, EntityComboBoxModel foreignKeyModel);

  /**
   * Selects the entity with the given primary key, if the entity is not available
   * in the model this method returns silently without changing the selection
   * @param primaryKey the primary key of the entity to select
   */
  void setSelectedEntityByKey(Entity.Key primaryKey);

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
  EntityComboBoxModel setStaticData(boolean staticData);

  /**
   * @return true if the data for this model should only be fetched once
   * @see #forceRefresh()
   */
  boolean isStaticData();

  /**
   * Set to true if this combo box model should respond to entity edit events, by adding inserted items,
   * updating updated items and removing deleted ones.
   * @param listenToEditEvents if true then this model listens to entity edit events
   * @return this EntityComboBoxModel instance
   * @see EntityEditEvents
   */
  EntityComboBoxModel setListenToEditEvents(boolean listenToEditEvents);

  /**
   * True if this combo box model responds to entity edit events, by adding inserted items,
   * updating updated items and removing deleted ones.
   * @return true if this combo box model listens edit events
   * @see EntityEditEvents
   */
  boolean isListenToEditEvents();

  /**
   * Sets the condition provider to use when querying data
   * @param selectConditionProvider the condition provider
   */
  void setSelectConditionProvider(Condition.Provider selectConditionProvider);

  /**
   * @return the select condition provider, null if none is specified
   */
  Condition.Provider getSelectConditionProvider();

  /**
   * @param listener a listener to be notified each time this model is refreshed
   */
  void addRefreshListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeRefreshListener(EventListener listener);
}
