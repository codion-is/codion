/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.db.ResultPacker;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.BlobProperty;
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
  private final Entity.Definition definition;
  private final List<ColumnProperty> columnProperties;

  /**
   * @param domain the Domain model
   * @param definition the entity definition
   * @param columnProperties the column properties in the same order as they appear in the ResultSet
   */
  EntityResultPacker(final Domain domain, final Entity.Definition definition,
                     final List<ColumnProperty> columnProperties) {
    this.domain = domain;
    this.definition = definition;
    this.columnProperties = columnProperties;
  }

  @Override
  public Entity fetch(final ResultSet resultSet) throws SQLException {
    final List<BlobProperty> lazyLoadedBlobProperties = definition.getLazyLoadedBlobProperties();
    final List<TransientProperty> transientProperties = definition.getTransientProperties();
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
        throw new SQLException("Exception fetching: " + property + ", entity: " +
                definition.getEntityId() + " [" + e.getMessage() + "]", e);
      }
    }
    for (int i = 0; i < lazyLoadedBlobProperties.size(); i++) {
      final BlobProperty blobProperty = lazyLoadedBlobProperties.get(i);
      if (!values.containsKey(blobProperty)) {
        values.put(blobProperty, null);
      }
    }

    return domain.entity(definition, values, null);
  }
}
