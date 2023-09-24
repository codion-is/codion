/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.BlobColumnDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles packing Entity query results.
 */
final class EntityResultPacker implements ResultPacker<Entity> {

  private static final Function<EntityDefinition, List<TransientAttributeDefinition<?>>> INIT_NON_DERIVED_TRANSIENT_ATTRIBUTES =
          EntityResultPacker::initializeNonDerivedTransientAttributes;
  private static final Function<EntityDefinition, List<ColumnDefinition<byte[]>>> INIT_LAZY_LOADED_BLOB_COLUMNS =
          EntityResultPacker::initializeLazyLoadedBlobColumns;
  private static final Map<EntityDefinition, List<TransientAttributeDefinition<?>>> NON_DERIVED_TRANSIENT_ATTRIBUTES = new ConcurrentHashMap<>();
  private static final Map<EntityDefinition, List<ColumnDefinition<byte[]>>> LAZY_LOADED_BLOB_COLUMNS = new ConcurrentHashMap<>();

  private final EntityDefinition entityDefinition;
  private final List<ColumnDefinition<?>> columnDefinitions;

  /**
   * @param entityDefinition the entity definition
   * @param columnDefinitions the column definitions in the same order as they appear in the ResultSet
   */
  EntityResultPacker(EntityDefinition entityDefinition, List<ColumnDefinition<?>> columnDefinitions) {
    this.entityDefinition = entityDefinition;
    this.columnDefinitions = columnDefinitions;
  }

  @Override
  public Entity get(ResultSet resultSet) throws SQLException {
    Map<Attribute<?>, Object> values = new HashMap<>(columnDefinitions.size());
    addResultSetValues(resultSet, values);
    addTransientNullValues(values);
    addLazyLoadedBlobNullValues(values);

    return entityDefinition.entity(values);
  }

  private void addResultSetValues(ResultSet resultSet, Map<Attribute<?>, Object> values) throws SQLException {
    for (int i = 0; i < columnDefinitions.size(); i++) {
      ColumnDefinition<Object> columnDefinition = (ColumnDefinition<Object>) columnDefinitions.get(i);
      try {
        values.put(columnDefinition.attribute(), columnDefinition.prepareValue(columnDefinition.get(resultSet, i + 1)));
      }
      catch (Exception e) {
        throw new SQLException("Exception fetching: " + columnDefinition + ", entity: " +
                entityDefinition.entityType() + " [" + e.getMessage() + "]", e);
      }
    }
  }

  private void addTransientNullValues(Map<Attribute<?>, Object> values) {
    List<TransientAttributeDefinition<?>> nonDerivedTransientAttributes =
            NON_DERIVED_TRANSIENT_ATTRIBUTES.computeIfAbsent(entityDefinition, INIT_NON_DERIVED_TRANSIENT_ATTRIBUTES);
    if (!nonDerivedTransientAttributes.isEmpty()) {
      for (TransientAttributeDefinition<?> attribute : nonDerivedTransientAttributes) {
        values.put(attribute.attribute(), null);
      }
    }
  }

  private void addLazyLoadedBlobNullValues(Map<Attribute<?>, Object> values) {
    List<ColumnDefinition<byte[]>> lazyLoadedBlobColumns =
            LAZY_LOADED_BLOB_COLUMNS.computeIfAbsent(entityDefinition, INIT_LAZY_LOADED_BLOB_COLUMNS);
    if (!lazyLoadedBlobColumns.isEmpty()) {
      for (ColumnDefinition<byte[]> column : lazyLoadedBlobColumns) {
        values.putIfAbsent(column.attribute(), null);
      }
    }
  }

  private static List<TransientAttributeDefinition<?>> initializeNonDerivedTransientAttributes(EntityDefinition entityDefinition) {
    return entityDefinition.attributeDefinitions().stream()
            .filter(TransientAttributeDefinition.class::isInstance)
            .map(attributeDefinition -> (TransientAttributeDefinition<?>) attributeDefinition)
            .filter(transientAttributeDefinition -> !transientAttributeDefinition.isDerived())
            .collect(Collectors.toList());
  }

  private static List<ColumnDefinition<byte[]>> initializeLazyLoadedBlobColumns(EntityDefinition entityDefinition) {
    return entityDefinition.columnDefinitions().stream()
            .filter(column -> column.attribute().type().isByteArray())
            .map(column -> (ColumnDefinition<byte[]>) column)
            .filter(column -> !(column instanceof BlobColumnDefinition) || !((BlobColumnDefinition) column).isEagerlyLoaded())
            .collect(Collectors.toList());
  }
}
