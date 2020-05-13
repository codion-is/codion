/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.local;

import dev.codion.common.db.result.ResultPacker;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.entity.EntityDefinition;
import dev.codion.framework.domain.property.ColumnProperty;
import dev.codion.framework.domain.property.DerivedProperty;
import dev.codion.framework.domain.property.Property;
import dev.codion.framework.domain.property.TransientProperty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles packing Entity query results.
 */
final class EntityResultPacker implements ResultPacker<Entity> {

  private final EntityDefinition definition;
  private final List<ColumnProperty> columnProperties;

  /**
   * @param definition the entity definition
   * @param columnProperties the column properties in the same order as they appear in the ResultSet
   */
  EntityResultPacker(final EntityDefinition definition, final List<ColumnProperty> columnProperties) {
    this.definition = definition;
    this.columnProperties = columnProperties;
  }

  @Override
  public Entity fetch(final ResultSet resultSet) throws SQLException {
    final Map<Property, Object> values = new HashMap<>(columnProperties.size());
    addResultSetValues(resultSet, values);
    addTransientNullValues(values);
    addLazyLoadedBlobNullValues(values);

    return definition.entity(values, null);
  }

  private void addResultSetValues(final ResultSet resultSet, final Map<Property, Object> values) throws SQLException {
    for (int i = 0; i < columnProperties.size(); i++) {
      final ColumnProperty property = columnProperties.get(i);
      try {
        values.put(property, property.prepareValue(property.fetchValue(resultSet, i + 1)));
      }
      catch (final Exception e) {
        throw new SQLException("Exception fetching: " + property + ", entity: " +
                definition.getEntityId() + " [" + e.getMessage() + "]", e);
      }
    }
  }

  private void addTransientNullValues(final Map<Property, Object> values) {
    final List<TransientProperty> transientProperties = definition.getTransientProperties();
    for (int i = 0; i < transientProperties.size(); i++) {
      final TransientProperty transientProperty = transientProperties.get(i);
      if (!(transientProperty instanceof DerivedProperty)) {
        values.put(transientProperty, null);
      }
    }
  }

  private void addLazyLoadedBlobNullValues(final Map<Property, Object> values) {
    final List<ColumnProperty> lazyLoadedBlobProperties = definition.getLazyLoadedBlobProperties();
    for (int i = 0; i < lazyLoadedBlobProperties.size(); i++) {
      final ColumnProperty blobProperty = lazyLoadedBlobProperties.get(i);
      if (!values.containsKey(blobProperty)) {
        values.put(blobProperty, null);
      }
    }
  }
}
