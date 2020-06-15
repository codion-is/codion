/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.TransientProperty;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
   * @return the entity type
   */
  EntityType<?> getEntityType();

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
   * @return the name of the domain this entity type belongs to
   */
  String getDomainName();

  /**
   * @return the validator for this entity type
   */
  EntityValidator getValidator();

  /**
   * @return the caption to use when presenting entities of this type
   */
  String getCaption();

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
   * @return true if this entity has a defined primary key
   */
  boolean hasPrimaryKey();

  /**
   * @return true if this entity contains any attribute which values are derived from other attribute
   */
  boolean hasDerivedAttributes();

  /**
   * Returns true if this entity contains attributes which values are derived from the value of the given attribute
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return true if any attribute values are derived from the given attribute
   */
  <T> boolean hasDerivedAttributes(Attribute<T> attribute);

  /**
   * Returns the attributes which values are derived from the value of the given attribute,
   * an empty collection if no such derived attributes exist
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a collection containing the attributes which are derived from the given attribute
   */
  <T> Collection<Attribute<?>> getDerivedAttributes(Attribute<T> attribute);

  /**
   * Returns a list containing all primary key attributes associated with this entity type.
   * If the entity has no primary key attributes defined, an empty list is returned.
   * @return the primary key attributes of this entity type, sorted by primary key column index
   */
  List<Attribute<?>> getPrimaryKeyAttributes();

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
   * @param <T> the attribute type
   * @return true if this entity type has any denormalized properties associated with the give foreign key
   */
  <T> boolean hasDenormalizedProperties(Attribute<T> foreignKeyAttribute);

  /**
   * Retrieves the denormalized properties which values originate from the entity referenced by the given foreign key property
   * @param foreignKeyAttribute the foreign key attribute
   * @param <T> the attribute type
   * @return a list containing the denormalized properties which values originate from the entity
   * referenced by the given foreign key property
   */
  <T> List<DenormalizedProperty<?>> getDenormalizedProperties(Attribute<T> foreignKeyAttribute);

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
   * the entity identified by {@code entityType}
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
   * the entity of type {@code entityType}
   */
  List<ColumnProperty<?>> getWritableColumnProperties(boolean includePrimaryKeyProperties, boolean includeNonUpdatable);

  /**
   * @return a list containing all updatable properties associated with the given entityType
   */
  List<Property<?>> getUpdatableProperties();

  /**
   * Returns the selectable {@link ColumnProperty}s identified
   * by the attributes in {@code attributes}
   * @param attributes the ids of the properties to retrieve
   * @return a list containing all column properties found in the entity identified by {@code entityType},
   * that is, properties that map to database columns, an empty list if none exist
   */
  List<ColumnProperty<?>> getSelectableColumnProperties(List<Attribute<?>> attributes);

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param foreignEntityType the id of the referenced entity
   * @return a List containing the properties, an empty list is returned in case no foreign key references are found
   */
  List<ForeignKeyProperty> getForeignKeyReferences(EntityType<?> foreignEntityType);

  /**
   * @param attribute the attribute
   * @return the Property.ForeignKeyProperty with the given attribute
   * @throws IllegalArgumentException in case no such property exists
   */
  ForeignKeyProperty getForeignKeyProperty(Attribute<Entity> attribute);

  /**
   * @param columnAttribute the column attribute
   * @param <T> the attribute type
   * @return the ForeignKeyProperties based on the given column property
   */
  <T> List<ForeignKeyProperty> getForeignKeyProperties(Attribute<T> columnAttribute);

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
  Entity entity(Key key);

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
  Entity entity(Function<Attribute<?>, Object> valueProvider);

  /**
   * Creates a new {@link Entity} instance based on this definition
   * @param values the values
   * @param originalValues the original values
   * @return a new {@link Entity} instance
   * @throws IllegalArgumentException in case any of the properties are not part of the entity.
   */
  Entity entity(Map<Attribute<?>, Object> values, Map<Attribute<?>, Object> originalValues);

  /**
   * Creates a new {@link Key} instance based on this definition
   * @return a new {@link Key} instance
   */
  Key key();

  /**
   * Creates a new {@link Key} instance based on this definition, initialised with the given value
   * @param value the key value, assumes a single integer key
   * @return a new {@link Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or value is null
   */
  Key key(Integer value);

  /**
   * Creates a new {@link Key} instance based on this definition, initialised with the given value
   * @param value the key value, assumes a single long key
   * @return a new {@link Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or value is null
   */
  Key key(Long value);

  /**
   * Returns the Attribute for the getter this method represents,
   * null if none exists.
   * @param method the method
   * @return the getter attribute
   */
  Attribute<?> getGetterAttribute(Method method);

  /**
   * Returns the Attribute for the setter this method represents,
   * null if none exists.
   * @param method the method
   * @return the setter attribute
   */
  Attribute<?> getSetterAttribute(Method method);

  /**
   * Provides {@link EntityDefinition}s for a domain model.
   */
  interface Provider {

    /**
     * Returns the {@link EntityDefinition} for the given entityType
     * @param entityType the entityType
     * @return the entity definition
     * @throws IllegalArgumentException in case the definition is not found
     */
    EntityDefinition getDefinition(EntityType<?> entityType);

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
  }
}
