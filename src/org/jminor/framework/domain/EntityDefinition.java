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

  String getEntityID();

  String getTableName();

  String getCaption();

  EntityDefinition setCaption(final String caption);

  boolean isLargeDataset();

  EntityDefinition setLargeDataset(final boolean largeDataset);

  boolean isReadOnly();

  EntityDefinition setReadOnly(final boolean readOnly);

  IdSource getIdSource();

  EntityDefinition setIdSource(final IdSource idSource);

  String getIdValueSource();

  EntityDefinition setIdValueSource(final String idValueSource);

  String getOrderByClause();

  EntityDefinition setOrderByClause(final String orderByClause);

  String getSelectTableName();

  EntityDefinition setSelectTableName(final String selectTableName);

  String getSelectQuery();

  EntityDefinition setSelectQuery(final String selectQuery);

  ValueMap.ToString<String> getStringProvider();

  EntityDefinition setStringProvider(final ValueMap.ToString<String> stringProvider);

  boolean isRowColoring();

  EntityDefinition setRowColoring(final boolean rowColoring);

  List<String> getSearchPropertyIDs();

  EntityDefinition setSearchPropertyIDs(final String... searchPropertyIDs);

  Map<String, Property> getProperties();

  boolean hasLinkedProperties(final String propertyID);

  Collection<String> getLinkedPropertyIDs(final String propertyID);

  List<Property.PrimaryKeyProperty> getPrimaryKeyProperties();

  String getSelectColumnsString();

  List<Property> getVisibleProperties();

  List<Property.ColumnProperty> getColumnProperties();

  List<Property.TransientProperty> getTransientProperties();

  List<Property.ForeignKeyProperty> getForeignKeyProperties();

  boolean hasDenormalizedProperties();

  boolean hasDenormalizedProperties(final String foreignKeyPropertyID);

  Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String foreignKeyPropertyID);
}
