/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.result.ResultPacker;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.TransientProperty;

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
  private final List<ColumnProperty<?>> columnProperties;

  /**
   * @param definition the entity definition
   * @param columnProperties the column properties in the same order as they appear in the ResultSet
   */
  EntityResultPacker(EntityDefinition definition, List<ColumnProperty<?>> columnProperties) {
    this.definition = definition;
    this.columnProperties = columnProperties;
  }

  @Override
  public Entity fetch(ResultSet resultSet) throws SQLException {
    Map<Attribute<?>, Object> values = new HashMap<>(columnProperties.size());
    addResultSetValues(resultSet, values);
    addTransientNullValues(values);
    addLazyLoadedBlobNullValues(values);

    return definition.entity(values);
  }

  private void addResultSetValues(ResultSet resultSet, Map<Attribute<?>, Object> values) throws SQLException {
    for (int i = 0; i < columnProperties.size(); i++) {
      ColumnProperty<Object> property = (ColumnProperty<Object>) columnProperties.get(i);
      try {
        values.put(property.attribute(), property.prepareValue(property.fetchValue(resultSet, i + 1)));
      }
      catch (Exception e) {
        throw new SQLException("Exception fetching: " + property + ", entity: " +
                definition.type() + " [" + e.getMessage() + "]", e);
      }
    }
  }

  private void addTransientNullValues(Map<Attribute<?>, Object> values) {
    List<TransientProperty<?>> transientProperties = definition.transientProperties();
    for (int i = 0; i < transientProperties.size(); i++) {
      TransientProperty<?> transientProperty = transientProperties.get(i);
      if (!transientProperty.isDerived()) {
        values.put(transientProperty.attribute(), null);
      }
    }
  }

  private void addLazyLoadedBlobNullValues(Map<Attribute<?>, Object> values) {
    List<ColumnProperty<?>> lazyLoadedBlobProperties = definition.lazyLoadedBlobProperties();
    for (int i = 0; i < lazyLoadedBlobProperties.size(); i++) {
      ColumnProperty<?> blobProperty = lazyLoadedBlobProperties.get(i);
      if (!values.containsKey(blobProperty.attribute())) {
        values.put(blobProperty.attribute(), null);
      }
    }
  }
}
