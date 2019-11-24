/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.db.ResultPacker;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.TransientProperty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles packing Entity query results.
 */
final class EntityResultPacker implements ResultPacker<Entity> {

  private final Domain domain;
  private final String entityId;
  private final List<ColumnProperty> columnProperties;
  private final List<TransientProperty> transientProperties;

  /**
   * @param domain the Domain model
   * @param entityId the entityId of the entity to pack
   * @param columnProperties the column properties in the same order as they appear in the ResultSet
   * @param transientProperties the transient properties in the given entity type, if any
   */
  EntityResultPacker(final Domain domain, final String entityId,
                     final List<ColumnProperty> columnProperties,
                     final List<TransientProperty> transientProperties) {
    this.domain = domain;
    this.entityId = entityId;
    this.columnProperties = columnProperties;
    this.transientProperties = transientProperties;
  }

  @Override
  public Entity fetch(final ResultSet resultSet) throws SQLException {
    final Map<Property, Object> values = new HashMap<>(
            columnProperties.size() + transientProperties.size());
    for (int i = 0; i < transientProperties.size(); i++) {
      final TransientProperty transientProperty = transientProperties.get(i);
      if (!(transientProperty instanceof DerivedProperty)) {
        values.put(transientProperty, null);
      }
    }
    for (int i = 0; i < columnProperties.size(); i++) {
      final ColumnProperty property = columnProperties.get(i);
      try {
        values.put(property, property.fetchValue(resultSet, i + 1));
      }
      catch (final Exception e) {
        throw new SQLException("Exception fetching: " + property + ", entity: " + entityId + " [" + e.getMessage() + "]", e);
      }
    }

    return domain.entity(entityId, values, null);
  }
}
