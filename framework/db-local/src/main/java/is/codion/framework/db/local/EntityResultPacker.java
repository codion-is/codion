/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;

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
  private final List<ColumnDefinition<?>> columnDefinitions;

  /**
   * @param definition the entity definition
   * @param columnDefinitions the column definitions in the same order as they appear in the ResultSet
   */
  EntityResultPacker(EntityDefinition definition, List<ColumnDefinition<?>> columnDefinitions) {
    this.definition = definition;
    this.columnDefinitions = columnDefinitions;
  }

  @Override
  public Entity get(ResultSet resultSet) throws SQLException {
    Map<Attribute<?>, Object> values = new HashMap<>(columnDefinitions.size());
    addResultSetValues(resultSet, values);
    addTransientNullValues(values);
    addLazyLoadedBlobNullValues(values);

    return definition.entity(values);
  }

  private void addResultSetValues(ResultSet resultSet, Map<Attribute<?>, Object> values) throws SQLException {
    for (int i = 0; i < columnDefinitions.size(); i++) {
      ColumnDefinition<Object> columnDefinition = (ColumnDefinition<Object>) columnDefinitions.get(i);
      try {
        values.put(columnDefinition.attribute(), columnDefinition.prepareValue(columnDefinition.get(resultSet, i + 1)));
      }
      catch (Exception e) {
        throw new SQLException("Exception fetching: " + columnDefinition + ", entity: " +
                definition.entityType() + " [" + e.getMessage() + "]", e);
      }
    }
  }

  private void addTransientNullValues(Map<Attribute<?>, Object> values) {
    List<TransientAttributeDefinition<?>> transientDefinitions = definition.transientAttributeDefinitions();
    for (int i = 0; i < transientDefinitions.size(); i++) {
      TransientAttributeDefinition<?> transientDefinition = transientDefinitions.get(i);
      if (!transientDefinition.isDerived()) {
        values.put(transientDefinition.attribute(), null);
      }
    }
  }

  private void addLazyLoadedBlobNullValues(Map<Attribute<?>, Object> values) {
    List<ColumnDefinition<byte[]>> lazyLoadedBlobColumns = definition.lazyLoadedBlobColumnDefinitions();
    for (int i = 0; i < lazyLoadedBlobColumns.size(); i++) {
      ColumnDefinition<?> blobDefinition = lazyLoadedBlobColumns.get(i);
      if (!values.containsKey(blobDefinition.attribute())) {
        values.put(blobDefinition.attribute(), null);
      }
    }
  }
}
