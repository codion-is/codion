/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.BlobColumnDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Handles packing Entity query results.
 */
final class EntityResultPacker implements ResultPacker<Entity> {

  // Lets reduce garbage production by keeping these lambdas around, instead of creating on each use
  private static final Predicate<TransientAttributeDefinition<?>> IS_NON_DERIVED =
          transientAttributeDefinition -> !transientAttributeDefinition.isDerived();
  private static final Predicate<AttributeDefinition<?>> IS_TRANSIENT =
          TransientAttributeDefinition.class::isInstance;
  private static final Function<AttributeDefinition<?>, TransientAttributeDefinition<?>> CAST_TO_TRANSIENT =
          attributeDefinition -> (TransientAttributeDefinition<?>) attributeDefinition;

  static final Predicate<ColumnDefinition<?>> IS_BYTE_ARRAY =
          column -> column.attribute().isByteArray();
  static final Function<ColumnDefinition<?>, ColumnDefinition<byte[]>> CAST_TO_BYTE_ARRAY_COLUMN =
          column -> (ColumnDefinition<byte[]>) column;
  static final Predicate<ColumnDefinition<byte[]>> LAZY_LOADED_BLOB =
          column -> !(column instanceof BlobColumnDefinition) || !((BlobColumnDefinition) column).isEagerlyLoaded();

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
    entityDefinition.attributeDefinitions().stream()
            .filter(IS_TRANSIENT)
            .map(CAST_TO_TRANSIENT)
            .filter(IS_NON_DERIVED)
            .forEach(transientAttributeDefinition -> values.put(transientAttributeDefinition.attribute(), null));
  }

  private void addLazyLoadedBlobNullValues(Map<Attribute<?>, Object> values) {
    entityDefinition.columnDefinitions().stream()
            .filter(IS_BYTE_ARRAY)
            .map(CAST_TO_BYTE_ARRAY_COLUMN)
            .filter(LAZY_LOADED_BLOB)
            .forEach(column -> values.putIfAbsent(column.attribute(), null));
  }
}
