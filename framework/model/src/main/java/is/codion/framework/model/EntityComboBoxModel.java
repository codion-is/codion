/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A ComboBoxModel based on {@link Entity} instances.
 */
public interface EntityComboBoxModel extends FilteredComboBoxModel<Entity> {

  /**
   * @return the type of the entity this combo box model is based on
   */
  EntityType getEntityType();

  /**
   * @return the connection provider used by this combo box model
   */
  EntityConnectionProvider getConnectionProvider();

  /**
   * @param primaryKey the primary key of the entity to fetch from this model
   * @return the entity with the given key if found in the model, an empty Optional otherwise
   */
  Optional<Entity> getEntity(Key primaryKey);

  /**
   * Use this method to retrieve the default foreign key filter include condition if you
   * want to add a custom {@link Predicate} to this model via {@link #setIncludeCondition(Predicate)}.
   * <pre>
   *   Predicate fkCondition = model.getForeignKeyIncludeCondition();
   *   model.setIncludeCondition(item -&gt; fkCondition.test(item) &amp;&amp; ...);
   * </pre>
   * @return the {@link Predicate} based on the foreign key filter entities
   * @see #setForeignKeyFilterEntities(ForeignKey, Collection)
   */
  Predicate<Entity> getForeignKeyIncludeCondition();

  /**
   * @param foreignKey the foreign key
   * @return the entities currently used to filter the contents of this model by foreign key, an empty collection for none
   */
  Collection<Entity> getForeignKeyFilterEntities(ForeignKey foreignKey);

  /**
   * Filters this combo box model so that only entities referencing the given foreign key entities
   * via the given foreign key property are shown.
   * @param foreignKey the foreign key
   * @param entities the entities, null or empty for none
   */
  void setForeignKeyFilterEntities(ForeignKey foreignKey, Collection<Entity> entities);

  /**
   * Specifies whether foreign key filtering should be strict or not.
   * When the filtering is strict only entities with the correct reference are included, that is,
   * entities with null values for the given foreign key are filtered.
   * Non-strict simply means that entities with null references are not filtered.
   * @param strictForeignKeyFiltering the value
   * @see #setForeignKeyFilterEntities(ForeignKey, Collection)
   */
  void setStrictForeignKeyFiltering(boolean strictForeignKeyFiltering);

  /**
   * @return true if strict foreign key filtering is enabled
   */
  boolean isStrictForeignKeyFiltering();

  /**
   * Returns a combo box model for selecting a foreign key value for filtering this model.
   * @param foreignKey the foreign key
   * @return a combo box model for selecting a filtering value for this combo box model
   * @see #linkForeignKeyFilterComboBoxModel(ForeignKey, EntityComboBoxModel)
   */
  EntityComboBoxModel createForeignKeyFilterComboBoxModel(ForeignKey foreignKey);

  /**
   * Returns a combo box model for selecting a foreign key value for using as a condition this model.
   * Note that each time the selection changes in the created model this model is refreshed.
   * @param foreignKey the foreign key
   * @return a combo box model for selecting a filtering value for this combo box model
   * @see #linkForeignKeyConditionComboBoxModel(ForeignKey, EntityComboBoxModel)
   */
  EntityComboBoxModel createForeignKeyConditionComboBoxModel(ForeignKey foreignKey);

  /**
   * Links the given combo box model representing master entities to this combo box model
   * so that selection in the master model filters this model according to the selected master entity
   * @param foreignKey the foreign key attribute
   * @param foreignKeyModel the combo box model to link
   */
  void linkForeignKeyFilterComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel);

  /**
   * Links the given combo box model representing master entities to this combo box model
   * so that selection in the master model refreshes this model with the selected master entity as condition
   * @param foreignKey the foreign key attribute
   * @param foreignKeyModel the combo box model to link
   */
  void linkForeignKeyConditionComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel);

  /**
   * Selects the entity with the given primary key, if the entity is not available
   * in the model this method returns silently without changing the selection
   * @param primaryKey the primary key of the entity to select
   */
  void setSelectedEntityByKey(Key primaryKey);

  /**
   * Forces a refresh of this model, disregarding the staticData directive
   * @see #setStaticData(boolean)
   */
  void forceRefresh();

  /**
   * Specifies whether this models data should be considered static, that is, only fetched once.
   * Note that {@link #forceRefresh()} disregards this directive.
   * @param staticData the value
   */
  void setStaticData(boolean staticData);

  /**
   * Specifies the attributes to include when selecting the entities to populate this model with.
   * Note that the primary key attribute values are always included.
   * An empty Collection indicates that all attributes should be selected.
   * @param selectAttributes the attributes to select
   * @throws IllegalArgumentException in case any of the given attributes is not part of the underlying entity type
   */
  void setSelectAttributes(Collection<Attribute<?>> selectAttributes);

  /**
   * @return true if the data for this model should only be fetched once
   * @see #forceRefresh()
   */
  boolean isStaticData();

  /**
   * Set to true if this combo box model should respond to entity edit events, by adding inserted items,
   * updating any updated items and removing deleted ones.
   * @param listenToEditEvents if true then this model listens to entity edit events
   * @see EntityEditEvents
   */
  void setListenToEditEvents(boolean listenToEditEvents);

  /**
   * True if this combo box model responds to entity edit events, by adding inserted items,
   * updating any updated items and removing deleted ones.
   * @return true if this combo box model listens edit events
   * @see EntityEditEvents
   */
  boolean isListenToEditEvents();

  /**
   * Sets the condition provider to use when querying data
   * @param selectConditionSupplier the condition supplier
   */
  void setSelectConditionSupplier(Supplier<Condition> selectConditionSupplier);

  /**
   * @return the select condition supplier, null if none is specified
   */
  Supplier<Condition> getSelectConditionSupplier();

  /**
   * Creates a {@link Value} linked to the selected entity via the value of the given attribute.
   * @param <V> the attribute type
   * @param attribute the attribute
   * @return a {@link Value} for selecting items by attribute value
   */
  <V> Value<V> selectorValue(Attribute<V> attribute);
}
