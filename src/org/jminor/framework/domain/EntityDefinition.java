/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.IdSource;
import org.jminor.common.model.valuemap.ValueMap;

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
   * @return true if the underlying table is "large"
   */
  boolean isLargeDataset();

  /**
   * Specifies whether or not this entity should be regarded as based on a large dataset,
   * which primarily means that combo box models should not be created based on this entity
   * @param largeDataset true if the underlying table is "large"
   * @return this EntityDefinition instance
   */
  EntityDefinition setLargeDataset(final boolean largeDataset);

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
   * @return true if this entity type should use specific row coloring when presented in table views
   */
  boolean isRowColoring();

  /**
   * Sets wether or not to use specific row coloring when presenting entities of this type in table views
   * @param rowColoring the rowColoring value
   * @return this EntityDefinition instance
   */
  EntityDefinition setRowColoring(final boolean rowColoring);

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
   * Returns the IDs of of the properties which values are linked to the value of the given property,
   * null if no linked properties exist
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
}
