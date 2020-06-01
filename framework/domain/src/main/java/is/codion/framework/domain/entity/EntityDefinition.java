/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.identity.Identity;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.TransientProperty;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Specifies a entity definition.
 */
public interface EntityDefinition extends Serializable {

  /**
   * Specifies whether it should be possible to define foreign keys referencing entities that have
   * not been defined, this can be disabled in cases where entities have circular references.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> STRICT_FOREIGN_KEYS = Configuration.booleanValue("codion.domain.strictForeignKeys", true);

  /**
   * @return the entityId
   */
  EntityIdentity getEntityId();

  /**
   * @return the name of the underlying table, with schema prefix if applicable
   */
  String getTableName();

  /**
   * Returns the {@link ConditionProvider} associated with the given id
   * @param conditionId the condition id
   * @return the condition provider associated with the given id
   * @throws IllegalArgumentException in case no ConditionProvider is associated with the given conditionId
   */
  ConditionProvider getConditionProvider(String conditionId);

  /**
   * @return the id of the domain this entity type belongs to
   */
  Identity getDomainId();

  /**
   * @return the validator for this entity type
   */
  EntityValidator getValidator();

  /**
   * @return the caption to use when presenting entities of this type
   */
  String getCaption();

  /**
   * Returns the bean class associated with this entity type
   * @param <V> the class type
   * @return the bean class
   */
  <V> Class<V> getBeanClass();

  /**
   * @return true if the underlying table is small enough for displaying the contents in a combo box
   */
  boolean isSmallDataset();

  /**
   * @return true if the data in the underlying table can be regarded as static
   */
  boolean isStaticData();

  /**
   * @return true if this entity type is read only
   */
  boolean isReadOnly();

  /**
   * @return the object responsible for generating primary key values for entities of this type
   * @see Builder#keyGenerator(KeyGenerator)
   */
  KeyGenerator getKeyGenerator();

  /**
   * Returns true if the value for the primary key of this entity is generated with a {@link KeyGenerator}.
   * @return true if the value for the primary key is generated
   * @see Builder#keyGenerator(KeyGenerator)
   */
  boolean isKeyGenerated();

  /**
   * @return the default order by clause to use when querying entities of this type
   */
  OrderBy getOrderBy();

  /**
   * @return the group by clause to use when querying entities of this type,
   * without the "group by" keywords
   */
  String getGroupByClause();

  /**
   * @return the having clause to use when querying entities of this type,
   * without the "having" keyword
   */
  String getHavingClause();

  /**
   * @return the name of the table to use when selecting entities of this type
   */
  String getSelectTableName();

  /**
   * @return the select query to use when selecting entities of this type
   */
  String getSelectQuery();

  /**
   * @return true if the select query, if any, contains a where clause
   */
  boolean selectQueryContainsWhereClause();

  /**
   * @return the object responsible for providing toString values for this entity type
   */
  Function<Entity, String> getStringProvider();

  /**
   * @return the comparator used when comparing this entity type to other entities
   */
  Comparator<Entity> getComparator();

  /**
   * @return a unmodifiable list view of the properties
   */
  List<Property<?>> getProperties();

  /**
   * @return a Set containing all the properties in this entity
   */
  Set<Property<?>> getPropertySet();

  /**
   * @return true if this entity has a defined primary key
   */
  boolean hasPrimaryKey();

  /**
   * @return true if this entity contains any properties which values are derived from other properties
   */
  boolean hasDerivedProperties();

  /**
   * Returns true if this entity contains properties which values are derived from the value of the given property
   * @param attribute the attribute
   * @return true if any properties are derived from the given property
   */
  boolean hasDerivedProperties(Attribute<?> attribute);

  /**
   * Returns the properties which values are derived from the value of the given property,
   * an empty collection if no such derived properties exist
   * @param attribute the attribute
   * @return a collection containing the properties which are derived from the given property
   */
  Collection<DerivedProperty<?>> getDerivedProperties(Attribute<?> attribute);

  /**
   * Returns a list containing all primary key properties associated with this entity type.
   * If the entity has no primary key properties defined, an empty list is returned.
   * @return the primary key properties of this entity type, sorted by primary key column index
   */
  List<ColumnProperty<?>> getPrimaryKeyProperties();

  /**
   * @return a list containing the visible properties for this entity type
   */
  List<Property<?>> getVisibleProperties();

  /**
   * @return a list containing the column-based properties for this entity type
   */
  List<ColumnProperty<?>> getColumnProperties();

  /**
   * Returns the default select column properties used when selecting this entity type,
   * this does not include properties where {@link ColumnProperty#isSelectable()} returns false
   * and {@link BlobProperty}s with {@link BlobProperty#isEagerlyLoaded()} returning true.
   * @return a list containing the default column properties to include in select queries
   */
  List<ColumnProperty<?>> getSelectableColumnProperties();

  /**
   * @return a list containing all lazy loaded blob properties for this entity type
   */
  List<ColumnProperty<?>> getLazyLoadedBlobProperties();

  /**
   * @return a list containing the non-column-based properties for this entity type
   */
  List<TransientProperty<?>> getTransientProperties();

  /**
   * @return a list containing the foreign key properties for this entity type
   */
  List<ForeignKeyProperty> getForeignKeyProperties();

  /**
   * Returns the {@link EntityDefinition} of the entity referenced by the given foreign key property.
   * @param foreignKeyAttribute the foreign key attribute
   * @return the definition of the referenced entity
   */
  EntityDefinition getForeignDefinition(Attribute<Entity> foreignKeyAttribute);

  /**
   * @return true if this entity type has any denormalized properties
   */
  boolean hasDenormalizedProperties();

  /**
   * @param foreignKeyAttribute the id of the foreign key property
   * @return true if this entity type has any denormalized properties associated with the give foreign key
   */
  boolean hasDenormalizedProperties(Attribute<?> foreignKeyAttribute);

  /**
   * Retrieves the denormalized properties which values originate from the entity referenced by the given foreign key property
   * @param foreignKeyAttribute the foreign key attribute
   * @return a list containing the denormalized properties which values originate from the entity
   * referenced by the given foreign key property
   */
  List<DenormalizedProperty<?>> getDenormalizedProperties(Attribute<?> foreignKeyAttribute);

  /**
   * Returns the properties to search by when searching for entities of this type by a string value
   * @return the properties to use when searching by string
   * @see ColumnProperty.Builder#searchProperty(boolean)
   */
  Collection<ColumnProperty<?>> getSearchProperties();

  /**
   * @param attribute the attribute
   * @return the column property associated with the attribute
   * @param <T> the attribute type
   * @throws IllegalArgumentException in case the attribute does not represent a {@link ColumnProperty}
   */
  <T> ColumnProperty<T> getColumnProperty(Attribute<T> attribute);

  /**
   * @param attribute the attribute
   * @return the property associated with {@code attribute}.
   * @param <T> the attribute type
   * @throws IllegalArgumentException in case no such property exists
   */
  <T> Property<T> getProperty(Attribute<T> attribute);

  /**
   * @param attribute the attribute
   * @return the primary key property associated with {@code attribute}.
   * @param <T> the attribute type
   * @throws IllegalArgumentException in case no such property exists
   */
  <T> ColumnProperty<T> getPrimaryKeyProperty(Attribute<T> attribute);

  /**
   * Returns the {@link Property}s identified by the attributes in {@code attributes}
   * @param attributes the ids of the properties to retrieve
   * @return a list containing the properties identified by {@code attributes}, found in
   * the entity identified by {@code entityId}
   */
  List<Property<?>> getProperties(Collection<Attribute<?>> attributes);

  /**
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return the column property associated with the attribute
   * @throws IllegalArgumentException in case the attribute does not represent a {@link ColumnProperty}
   * or if it is not selectable
   * @see ColumnProperty#isSelectable()
   */
  <T> ColumnProperty<T> getSelectableColumnProperty(Attribute<T> attribute);

  /**
   * Returns the {@link ColumnProperty}s identified
   * by the attributes in {@code attributes}
   * @param attributes the attributes which properties to retrieve
   * @return a list of column properties
   */
  List<ColumnProperty<?>> getColumnProperties(List<Attribute<?>> attributes);

  /**
   * @return true if the primary key of the given type of entity is comprised of a single integer value
   */
  boolean hasSingleIntegerPrimaryKey();

  /**
   * Retrieves the writable (non read-only) column properties comprising this entity type
   * @param includePrimaryKeyProperties if true primary key properties are included, non-updatable primary key properties
   * are only included if {@code includeNonUpdatable} is true
   * @param includeNonUpdatable if true then non updatable properties are included
   * @return a list containing the writable column properties (properties that map to database columns) comprising
   * the entity identified by {@code entityId}
   */
  List<ColumnProperty<?>> getWritableColumnProperties(boolean includePrimaryKeyProperties, boolean includeNonUpdatable);

  /**
   * @return a list containing all updatable properties associated with the given  entityId
   */
  List<Property<?>> getUpdatableProperties();

  /**
   * Returns the selectable {@link ColumnProperty}s identified
   * by the attributes in {@code attributes}
   * @param attributes the ids of the properties to retrieve
   * @return a list containing all column properties found in the entity identified by {@code entityId},
   * that is, properties that map to database columns, an empty list if none exist
   */
  List<ColumnProperty<?>> getSelectableColumnProperties(List<Attribute<?>> attributes);

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param foreignEntityId the id of the referenced entity
   * @return a List containing the properties, an empty list is returned in case no foreign key references are found
   */
  List<ForeignKeyProperty> getForeignKeyReferences(Identity foreignEntityId);

  /**
   * @param attribute the attribute
   * @return the Property.ForeignKeyProperty with the given attribute
   * @throws IllegalArgumentException in case no such property exists
   */
  ForeignKeyProperty getForeignKeyProperty(Attribute<Entity> attribute);

  /**
   * @param columnAttribute the column attribute
   * @return the ForeignKeyProperties based on the given column property
   */
  List<ForeignKeyProperty> getForeignKeyProperties(Attribute<?> columnAttribute);

  /**
   * Returns the color provider, never null
   * @return the color provider
   */
  ColorProvider getColorProvider();

  /**
   * Creates a new {@link Entity} instance based on this definition
   * @return a new {@link Entity} instance
   */
  Entity entity();

  /**
   * Creates a new {@link Entity} instance based on this definition
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  Entity entity(Entity.Key key);

  /**
   * Instantiates a new {@link Entity} using the values provided by {@code valueProvider}.
   * Values are fetched for {@link ColumnProperty} and its descendants, {@link ForeignKeyProperty}
   * and {@link TransientProperty} (excluding its descendants).
   * If a {@link ColumnProperty}s underlying column has a default value the property is
   * skipped unless the property itself has a default value, which then overrides the columns default value.
   * @param valueProvider provides the default value for a given property
   * @return the populated entity
   * @see ColumnProperty.Builder#columnHasDefaultValue(boolean)
   * @see ColumnProperty.Builder#defaultValue(Object)
   */
  Entity entity(Function<Property<?>, Object> valueProvider);

  /**
   * Creates a new {@link Entity} instance based on this definition
   * @param values the values
   * @param originalValues the original values
   * @return a new {@link Entity} instance
   * @throws IllegalArgumentException in case any of the properties are not part of the entity.
   */
  Entity entity(Map<Attribute<?>, Object> values, Map<Attribute<?>, Object> originalValues);

  /**
   * Creates a new {@link Entity.Key} instance based on this definition
   * @return a new {@link Entity.Key} instance
   */
  Entity.Key key();

  /**
   * Creates a new {@link Entity.Key} instance based on this definition, initialised with the given value
   * @param value the key value, assumes a single integer key
   * @return a new {@link Entity.Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or value is null
   */
  Entity.Key key(Integer value);

  /**
   * Creates a new {@link Entity.Key} instance based on this definition, initialised with the given value
   * @param value the key value, assumes a single long key
   * @return a new {@link Entity.Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or value is null
   */
  Entity.Key key(Long value);

  /**
   * Returns the {@link BeanHelper} associated with this entity type.
   * @param <V> the bean type
   * @return the bean helper
   */
  <V> BeanHelper<V> getBeanHelper();

  /**
   * Helps with transforming from entities to beans and back. Called after the default
   * transformation has finished.
   * Use one of these if the default bean transformation is not enough.
   * @param <V> the bean type
   */
  interface BeanHelper<V> extends Serializable {

    /**
     * Called after the default transformation has finished.
     * @param entity the entity
     * @param bean the bean
     * @return the entity
     */
    default V toBean(final Entity entity, final V bean) {
      return bean;
    }

    /**
     * Called after the default transformation has finished.
     * @param bean the bean
     * @param entity the entity
     * @return the bean
     */
    default Entity fromBean(final V bean, final Entity entity) {
      return entity;
    }
  }

  /**
   * Provides {@link EntityDefinition}s for a domain model.
   */
  interface Provider {

    /**
     * Returns the {@link EntityDefinition} for the given entityId
     * @param entityId the entityId
     * @return the entity definition
     * @throws IllegalArgumentException in case the definition is not found
     */
    EntityDefinition getDefinition(Identity entityId);

    /**
     * Returns all {@link EntityDefinition}s available in this provider
     * @return all entity definitions
     */
    Collection<EntityDefinition> getDefinitions();
  }

  /**
   * Builds a EntityDefinition
   */
  interface Builder {

    /**
     * @return the {@link EntityDefinition} instance
     */
    EntityDefinition get();

    /**
     * @param domainId the domain id
     * @return this {@link Builder} instance
     * @throws IllegalStateException in case the domain id has already been set
     */
    Builder domainId(Identity domainId);

    /**
     * @param validator the validator for this entity type
     * @return this {@link Builder} instance
     */
    Builder validator(EntityValidator validator);

    /**
     * Adds a {@link ConditionProvider} which provides a dynamic query condition string.
     * The condition string should not include the WHERE keyword and use the ?
     * substitution character where values should be inserted.
     * @param conditionId the condition id
     * @param conditionProvider the condition provider
     * @return this Entity.Definer instance
     */
    Builder conditionProvider(String conditionId, ConditionProvider conditionProvider);

    /**
     * @param colorProvider the background color provider
     * @return this {@link Builder} instance
     */
    Builder colorProvider(ColorProvider colorProvider);

    /**
     * Sets the caption for this entity type
     * @param caption the caption
     * @return this {@link Builder} instance
     */
    Builder caption(String caption);

    /**
     * Sets the bean class to associate with this entity type
     * @param beanClass the bean class
     * @param <V> the class type
     * @return this {@link Builder} instance
     */
    <V> Builder beanClass(Class<V> beanClass);

    /**
     * Specifies whether or not this entity should be regarded as based on a small dataset,
     * which primarily means that combo box models can be based on this entity.
     * This is false by default.
     * @param smallDataset true if the underlying table is small enough for displaying the contents in a combo box
     * @return this {@link Builder} instance
     */
    Builder smallDataset(boolean smallDataset);

    /**
     * Specifies whether or not this entity should be regarded as based on a static dataset, that is,
     * one that changes only infrequently.
     * This is false by default.
     * @param staticData true if the underlying table data is static
     * @return this {@link Builder} instance
     */
    Builder staticData(boolean staticData);

    /**
     * Sets the read only value, if true then it should not be possible to
     * insert, update or delete entities of this type
     * @param readOnly true if this entity type should be read only
     * @return this {@link Builder} instance
     */
    Builder readOnly(boolean readOnly);

    /**
     * Sets the primary key generator
     * @param keyGenerator the primary key generator
     * @return this {@link Builder} instance
     * @see #isKeyGenerated()
     */
    Builder keyGenerator(KeyGenerator keyGenerator);

    /**
     * Sets the order by clause for this entity type.
     * @param orderBy the order by clause
     * @return this {@link Builder} instance
     */
    Builder orderBy(OrderBy orderBy);

    /**
     * Sets the having clause for this entity type, this clause should not
     * include the "having" keyword.
     * @param havingClause the having clause
     * @return this {@link Builder} instance
     * @throws IllegalStateException in case a having clause has already been set,
     * for example automatically, based on grouping properties
     */
    Builder havingClause(String havingClause);

    /**
     * Sets the group by clause for this entity type, this clause should not
     * include the "group by" keywords.
     * @param groupByClause the group by clause
     * @return this {@link Builder} instance
     * @throws IllegalStateException in case a group by clause has already been set,
     * for example automatically, based on grouping properties
     * @see ColumnProperty.Builder#groupingColumn(boolean)
     */
    Builder groupByClause(String groupByClause);

    /**
     * Sets the name of the table to use when selecting entities of this type,
     * when it differs from the one used to update/insert, such as a view.
     * @param selectTableName the name of the table
     * @return this {@link Builder} instance
     */
    Builder selectTableName(String selectTableName);

    /**
     * Sets the select query to use when selecting entities of this type,
     * use with care. The order of the properties when defining the entity
     * must match the column order in the given query.
     * @param selectQuery the select query to use for this entity type
     * @param containsWhereClause true if the given query contains a where clause
     * @return this {@link Builder} instance
     */
    Builder selectQuery(String selectQuery, boolean containsWhereClause);

    /**
     * Sets the string provider, that is, the object responsible for providing toString values for this entity type
     * @param stringProvider the string provider
     * @return this {@link Builder} instance
     */
    Builder stringProvider(Function<Entity, String> stringProvider);

    /**
     * Sets the comparator to use when comparing this entity type to other entities
     * @param comparator the comparator
     * @return this {@link Builder} instance
     */
    Builder comparator(Comparator<Entity> comparator);

    /**
     * Sets the {@link BeanHelper} instance to use when transforming between entities and beans.
     * Called after the default transformation has finished.
     * @param beanHelper the bean helper
     * @param <V> the bean type
     * @return this {@link Builder} instance
     */
    <V> Builder beanHelper(BeanHelper<V> beanHelper);
  }
}
