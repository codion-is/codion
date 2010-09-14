/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.IdSource;
import org.jminor.common.model.valuemap.ValueMap;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A interface defining a entity.
 */
public interface EntityDefinition {

  /**
   * @return the entity ID
   */
  String getEntityID();

  /**
   * @return the name of the underlying table, with schema prefix if applicable
   */
  String getTableName();

  /**
   * @return the ID of the domain this entity type belongs to
   */
  String getDomainID();

  /**
   * Sets the domain ID for this entity type
   * @param domainID the domain ID
   * @return this EntityDefinition instance
   */
  EntityDefinition setDomainID(final String domainID);

  /**
   * @param toString the to string provider
   * @return this EntityDefinition instance
   */
  EntityDefinition setToStringProvider(final ValueMap.ToString<String> toString);

  /**
   * @param colorProvider the background color provider
   * @return this EntityDefinition instance
   */
  EntityDefinition setBackgroundColorProvider(final Entity.BackgroundColorProvider colorProvider);

  /**
   * @param validator the validator for this entity type
   * @return this EntityDefinition instance
   */
  EntityDefinition setValidator(final Entity.Validator validator);

  /**
   * @return the validator for this enitity type
   */
  Entity.Validator getValidator();

  /**
   * @return the caption to use when presenting entities of this type
   */
  String getCaption();

  /**
   * Sets the caption for this entity type
   * @param caption the caption
   * @return this EntityDefinition instance
   */
  EntityDefinition setCaption(final String caption);

  /**
   * @return true if the underlying table is small enough for displaying the contents in a combo box
   */
  boolean isSmallDataset();

  /**
   * Specifies whether or not this entity should be regarded as based on a small dataset,
   * which primarily means that combo box models can be based on this entity.
   * This is false by default.
   * @param smallDataset true if the underlying table is small enough for displaying the contents in a combo box
   * @return this EntityDefinition instance
   */
  EntityDefinition setSmallDataset(final boolean smallDataset);

  /**
   * @return true if this entity type is read only
   */
  boolean isReadOnly();

  /**
   * Sets the read only value
   * @param readOnly true if this entity type should be read only
   * @return this EntityDefinition instance
   */
  EntityDefinition setReadOnly(final boolean readOnly);

  /**
   * @return the IdSource specified for this entity type
   */
  IdSource getIdSource();

  /**
   * Sets the id source for this entity type, which specifies the primary key
   * generation strategy to use.
   * @param idSource the idSource
   * @return this EntityDefinition instance
   */
  EntityDefinition setIdSource(final IdSource idSource);

  /**
   * @return the id value source
   */
  String getIdValueSource();

  /**
   * Sets the id value source for this entity type, such as sequence or table name,
   * depending on the underlying primary key generation strategy.
   * @param idValueSource the id value source
   * @return this EntityDefinition instance
   */
  EntityDefinition setIdValueSource(final String idValueSource);

  /**
   * @return the order by clause to use when querying entities of this type,
   * without the "order by" keywords
   */
  String getOrderByClause();

  /**
   * Sets the order by clause for this entity type, this clause should not
   * include the "order by" keywords.
   * @param orderByClause the order by clause
   * @return this EntityDefinition instance
   */
  EntityDefinition setOrderByClause(final String orderByClause);

  /**
   * @return the name of the table to use when selecting entities of this type
   */
  String getSelectTableName();

  /**
   * Sets the name of the table to use when selecting entities of this type,
   * when it differs from the one used to update/insert, such as a view.
   * @param selectTableName the name of the table
   * @return this EntityDefinition instance
   */
  EntityDefinition setSelectTableName(final String selectTableName);

  /**
   * @return the select query to use when selecting entities of this type
   */
  String getSelectQuery();

  /**
   * Sets the select query to use when selecting entities of this type,
   * use with care.
   * @param selectQuery the select query to use for this entity type
   * @return this EntityDefinition instance
   */
  EntityDefinition setSelectQuery(final String selectQuery);

  /**
   * @return the object responsible for providing toString values for this entity type
   */
  ValueMap.ToString<String> getStringProvider();

  /**
   * Sets the string provider, that is, the object responsible for providing toString values for this entity type
   * @param stringProvider the string provider
   * @return this EntityDefinition instance
   */
  EntityDefinition setStringProvider(final ValueMap.ToString<String> stringProvider);

  /**
   * Sets the comparator to use when comparing this entity type to other entities
   * @param comparator the comparator
   * @return this EntityDefinition instance
   */
  EntityDefinition setComparator(final Entity.Comparator comparator);

  /**
   * @return the comparator used when comparing this entity type to other entities
   */
  Entity.Comparator getComparator();

  /**
   * @return a list of property IDs identifying the properties to use when performing
   * a default lookup for this entity type
   */
  List<String> getSearchPropertyIDs();

  /**
   * Sets the IDs of the properties to use when performing a default lookup for this entity type
   * @param searchPropertyIDs the search property IDs
   * @return this EntityDefinition instance
   */
  EntityDefinition setSearchPropertyIDs(final String... searchPropertyIDs);

  /**
   * @return the properties for this entity type
   */
  Map<String, Property> getProperties();

  /**
   * Returns true if this entity contains properties which values are linked to the value of the given property
   * @param propertyID the ID of the property
   * @return true if any properties are linked to the given property
   */
  boolean hasLinkedProperties(final String propertyID);

  /**
   * Returns the IDs of the properties which values are linked to the value of the given property,
   * an empty collection if no such linked properties exist
   * @param propertyID the ID of the property
   * @return a collection conaining the IDs of any properties which are linked to the given property
   */
  Collection<String> getLinkedPropertyIDs(final String propertyID);

  /**
   * @return the primary key properties of this entity type
   */
  List<Property.PrimaryKeyProperty> getPrimaryKeyProperties();

  /**
   * Retrieves the column list to use when constructing a select query for this entity type
   * @return the query column list, i.e. "col1, col2, col3,..."
   */
  String getSelectColumnsString();

  /**
   * @return a list containing the visible properties for this entity type
   */
  List<Property> getVisibleProperties();

  /**
   * @return a list containing the column-based properties for this entity type
   */
  List<Property.ColumnProperty> getColumnProperties();

  /**
   * @return a list containing the non-column-based properties for this entity type
   */
  List<Property.TransientProperty> getTransientProperties();

  /**
   * @return a list containing the foreign key properties for this entity type
   */
  List<Property.ForeignKeyProperty> getForeignKeyProperties();

  /**
   * @return true if this entity type has any denormalized properties
   */
  boolean hasDenormalizedProperties();

  /**
   * @param foreignKeyPropertyID the ID of the foreign key property
   * @return true if this entity type has any denormalized properties associated with the give foreign key
   */
  boolean hasDenormalizedProperties(final String foreignKeyPropertyID);

  /**
   * Retrieves the denormalized properties which values originate from the entity referenced by the given foreign key property
   * @param foreignKeyPropertyID the foreign key property ID
   * @return a collection containing the denormalized properties which values originate from the entity
   * referenced by the given foreign key property
   */
  Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String foreignKeyPropertyID);

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
   * @return the background color to use for this entity and property
   */
  Color getBackgroundColor(final Entity entity, final Property property);
}
