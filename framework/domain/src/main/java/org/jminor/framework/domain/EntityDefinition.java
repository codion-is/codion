/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.domain.property.BlobProperty;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DenormalizedProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.TransientProperty;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Specifies a entity definition.
 */
public interface EntityDefinition extends Serializable {

  /**
   * Specifies that it should not be possible to define foreign keys referencing entities that have
   * not been defined, this can be disabled in case of entities with circular references<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> STRICT_FOREIGN_KEYS = Configuration.booleanValue("jminor.domain.strictForeignKeys", true);

  /**
   * @return the entity ID
   */
  String getEntityId();

  /**
   * @return the name of the underlying table, with schema prefix if applicable
   */
  String getTableName();

  /**
   * Returns the {@link Entity.ConditionProvider} associated with the given id
   * @param conditionId the condition id
   * @return the condition provider associated with the given id
   * @throws IllegalArgumentException in case no ConditionProvider is associated with the given conditionId
   */
  Entity.ConditionProvider getConditionProvider(final String conditionId);

  /**
   * @return the ID of the domain this entity type belongs to
   */
  String getDomainId();

  /**
   * @return the validator for this entity type
   */
  Entity.Validator getValidator();

  /**
   * @return the caption to use when presenting entities of this type
   */
  String getCaption();

  /**
   * Returns the bean class associated with this entity type
   * @return the bean class
   */
  Class getBeanClass();

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
   */
  KeyGenerator getKeyGenerator();

  /**
   * Returns true if the value for the primary key of this entity is generated, either by the framework,
   * such as values queried from sequences or set by triggers. If not the primary key value must be set manually
   * before the entity is inserted.
   * @return true if the value for the primary key is generated
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
  Entity.ToString getStringProvider();

  /**
   * @return the comparator used when comparing this entity type to other entities
   */
  Comparator<Entity> getComparator();

  /**
   * @return a collection of property IDs identifying the properties to use when performing
   * a default lookup for this entity type
   */
  Collection<String> getSearchPropertyIds();

  /**
   * @return a Set containing all the properties in this entity
   */
  Set<Property> getPropertySet();

  /**
   * @return a unmodifiable list view of the properties
   */
  List<Property> getProperties();

  /**
   * @return true if this entity contains any properties which values are derived from other properties
   */
  boolean hasDerivedProperties();

  /**
   * Returns true if this entity contains properties which values are derived from the value of the given property
   * @param propertyId the ID of the property
   * @return true if any properties are derived from the given property
   */
  boolean hasDerivedProperties(final String propertyId);

  /**
   * Returns the properties which values are derived from the value of the given property,
   * an empty collection if no such derived properties exist
   * @param propertyId the ID of the property
   * @return a collection containing the properties which are derived from the given property
   */
  Collection<DerivedProperty> getDerivedProperties(final String propertyId);

  /**
   * @return the primary key properties of this entity type, sorted by primary key column index
   */
  List<ColumnProperty> getPrimaryKeyProperties();

  /**
   * @return a list containing the visible properties for this entity type
   */
  List<Property> getVisibleProperties();

  /**
   * @return a list containing the column-based properties for this entity type
   */
  List<ColumnProperty> getColumnProperties();

  /**
   * Returns the default select column properties used when selecting this entity type,
   * this does not include properties where {@link ColumnProperty#isSelectable()} returns false
   * and {@link BlobProperty}s with {@link BlobProperty#isLazyLoaded()} returning false.
   * @return a list containing the column properties to include in select queries
   */
  List<ColumnProperty> getSelectableColumnProperties();

  /**
   * @return a list containing all lazy loaded blob properties for this entity type
   */
  List<BlobProperty> getLazyLoadedBlobProperties();

  /**
   * @return a list containing the non-column-based properties for this entity type
   */
  List<TransientProperty> getTransientProperties();

  /**
   * @return a list containing the foreign key properties for this entity type
   */
  List<ForeignKeyProperty> getForeignKeyProperties();

  /**
   * @return true if this entity type has any denormalized properties
   */
  boolean hasDenormalizedProperties();

  /**
   * @param foreignKeyPropertyId the ID of the foreign key property
   * @return true if this entity type has any denormalized properties associated with the give foreign key
   */
  boolean hasDenormalizedProperties(final String foreignKeyPropertyId);

  /**
   * Retrieves the denormalized properties which values originate from the entity referenced by the given foreign key property
   * @param foreignKeyPropertyId the foreign key property ID
   * @return a list containing the denormalized properties which values originate from the entity
   * referenced by the given foreign key property
   */
  List<DenormalizedProperty> getDenormalizedProperties(final String foreignKeyPropertyId);

  /**
   * Returns the properties to search by when looking up entities of the type identified by {@code entityId}
   * @return the properties to use when searching
   * @see Builder#setSearchPropertyIds(String...)
   */
  Collection<ColumnProperty> getSearchProperties();

  /**
   * @param propertyId the property id
   * @return the column property identified by property id
   * @throws IllegalArgumentException in case the propertyId does not represent a {@link ColumnProperty}
   */
  ColumnProperty getColumnProperty(final String propertyId);

  /**
   * @param propertyId the property id
   * @return the property identified by {@code propertyId} in the entity identified by {@code entityId}
   * @throws IllegalArgumentException in case no such property exists
   */
  Property getProperty(final String propertyId);

  /**
   * @param propertyId the property id
   * @return the primary key property identified by {@code propertyId} in the entity identified by {@code entityId}
   * @throws IllegalArgumentException in case no such property exists
   */
  ColumnProperty getPrimaryKeyProperty(String propertyId);

  /**
   * Returns the {@link Property}s identified by the propertyIds in {@code propertyIds}
   * @param propertyIds the ids of the properties to retrieve
   * @return a list containing the properties identified by {@code propertyIds}, found in
   * the entity identified by {@code entityId}
   */
  List<Property> getProperties(final Collection<String> propertyIds);

  /**
   * @param propertyId the property id
   * @return the column property identified by property id
   * @throws IllegalArgumentException in case the propertyId does not represent a {@link ColumnProperty}
   * or if it is not selectable
   * @see ColumnProperty#isSelectable()
   */
  ColumnProperty getSelectableColumnProperty(final String propertyId);

  /**
   * Returns the {@link ColumnProperty}s identified
   * by the propertyIds in {@code propertyIds}
   * @param propertyIds the ids of the properties to retrieve
   * @return a list containing all column properties found in the entity identified by {@code entityId},
   * that is, properties that map to database columns, an empty list if none exist
   */
  List<ColumnProperty> getColumnProperties(final List<String> propertyIds);

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
  List<ColumnProperty> getWritableColumnProperties(final boolean includePrimaryKeyProperties,
                                                   final boolean includeNonUpdatable);
  /**
   * @return a list containing all updatable properties associated with the given entity id
   */
  List<Property> getUpdatableProperties();

  /**
   * Returns the selectable {@link ColumnProperty}s identified
   * by the propertyIds in {@code propertyIds}
   * @param propertyIds the ids of the properties to retrieve
   * @return a list containing all column properties found in the entity identified by {@code entityId},
   * that is, properties that map to database columns, an empty list if none exist
   */
  List<ColumnProperty> getSelectableColumnProperties(final List<String> propertyIds);

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param foreignEntityId the id of the referenced entity
   * @return a List containing the properties, an empty list is returned in case no foreign key references are found
   */
  List<ForeignKeyProperty> getForeignKeyReferences(final String foreignEntityId);

  /**
   * @param propertyId the property id
   * @return the Property.ForeignKeyProperty with the given propertyId
   * @throws IllegalArgumentException in case no such property exists
   */
  ForeignKeyProperty getForeignKeyProperty(final String propertyId);

  /**
   * @param columnPropertyId the column property id
   * @return the ForeignKeyProperties based on the given column property
   */
  List<ForeignKeyProperty> getForeignKeyProperties(final String columnPropertyId);

  /**
   * Compares the given entities.
   * @param entity the first entity
   * @param entityToCompare the second entity
   * @return the compare result
   */
  int compareTo(final Entity entity, final Entity entityToCompare);

  /**
   * @param entity the entity
   * @return a string representation of the given entity
   */
  String toString(final Entity entity);

  /**
   * @param entity the entity
   * @param property the property
   * @return the background color to use for this entity and property, null if none is specified
   */
  Object getColor(final Entity entity, final Property property);

  /**
   * Provides {@link EntityDefinition}s
   */
  interface Provider {

    /**
     * Returns the {@link EntityDefinition} for the given entityId
     * @param entityId the entityId
     * @return the entity definition
     */
    EntityDefinition getDefinition(final String entityId);
  }

  /**
   * Builds a Entity.Definition
   */
  interface Builder {

    /**
     * @param validator the validator for this entity type
     * @return this {@link Builder} instance
     */
    Builder setValidator(final Entity.Validator validator);

    /**
     * Adds a {@link Entity.ConditionProvider} which provides a dynamic query condition string.
     * The condition string should not include the WHERE keyword and use the ?
     * substitution character where values should be inserted.
     * @param conditionId the condition id
     * @param conditionProvider the condition provider
     * @return this Entity.Definer instance
     */
    Builder addConditionProvider(final String conditionId, final Entity.ConditionProvider conditionProvider);

    /**
     * @param colorProvider the background color provider
     * @return this {@link Builder} instance
     */
    Builder setColorProvider(final Entity.ColorProvider colorProvider);

    /**
     * Sets the caption for this entity type
     * @param caption the caption
     * @return this {@link Builder} instance
     */
    Builder setCaption(final String caption);

    /**
     * Sets the bean class to associate with this entity type
     * @param beanClass the bean class
     * @return this {@link Builder} instance
     */
    Builder setBeanClass(final Class beanClass);

    /**
     * Specifies whether or not this entity should be regarded as based on a small dataset,
     * which primarily means that combo box models can be based on this entity.
     * This is false by default.
     * @param smallDataset true if the underlying table is small enough for displaying the contents in a combo box
     * @return this {@link Builder} instance
     */
    Builder setSmallDataset(final boolean smallDataset);

    /**
     * Specifies whether or not this entity should be regarded as based on a static dataset, that is,
     * one that changes only infrequently.
     * This is false by default.
     * @param staticData true if the underlying table data is static
     * @return this {@link Builder} instance
     */
    Builder setStaticData(final boolean staticData);

    /**
     * Sets the read only value, if true then it should not be possible to
     * insert, update or delete entities of this type
     * @param readOnly true if this entity type should be read only
     * @return this {@link Builder} instance
     */
    Builder setReadOnly(final boolean readOnly);

    /**
     * Sets the primary key generator
     * @param keyGenerator the primary key generator
     * @return this {@link Builder} instance
     */
    Builder setKeyGenerator(final KeyGenerator keyGenerator);

    /**
     * Sets the order by clause for this entity type.
     * @param orderBy the order by clause
     * @return this {@link Builder} instance
     */
    Builder setOrderBy(final OrderBy orderBy);

    /**
     * Sets the having clause for this entity type, this clause should not
     * include the "having" keyword.
     * @param havingClause the having clause
     * @return this {@link Builder} instance
     * @throws IllegalStateException in case a having clause has already been set,
     * for example automatically, based on grouping properties
     */
    Builder setHavingClause(final String havingClause);

    /**
     * Sets the group by clause for this entity type, this clause should not
     * include the "group by" keywords.
     * @param groupByClause the group by clause
     * @return this {@link Builder} instance
     * @throws IllegalStateException in case a group by clause has already been set,
     * for example automatically, based on grouping properties
     * @see ColumnProperty.Builder#setGroupingColumn(boolean)
     */
    Builder setGroupByClause(final String groupByClause);

    /**
     * Sets the name of the table to use when selecting entities of this type,
     * when it differs from the one used to update/insert, such as a view.
     * @param selectTableName the name of the table
     * @return this {@link Builder} instance
     */
    Builder setSelectTableName(final String selectTableName);

    /**
     * Sets the select query to use when selecting entities of this type,
     * use with care. The order of the properties when defining the entity
     * must match the column order in the given query.
     * @param selectQuery the select query to use for this entity type
     * @param containsWhereClause true if the given query contains a where clause
     * @return this {@link Builder} instance
     */
    Builder setSelectQuery(final String selectQuery, final boolean containsWhereClause);

    /**
     * Sets the string provider, that is, the object responsible for providing toString values for this entity type
     * @param stringProvider the string provider
     * @return this {@link Builder} instance
     */
    Builder setStringProvider(final Entity.ToString stringProvider);

    /**
     * Sets the comparator to use when comparing this entity type to other entities
     * @param comparator the comparator
     * @return this {@link Builder} instance
     */
    Builder setComparator(final Comparator<Entity> comparator);

    /**
     * Sets the IDs of the properties to use when performing a default lookup for this entity type
     * @param searchPropertyIds the search property IDs
     * @return this {@link Builder} instance
     */
    Builder setSearchPropertyIds(final String... searchPropertyIds);
  }
}
