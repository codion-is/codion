/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.util.HashMap;
import java.util.Map;

final class DefaultKeyBuilder implements Entity.Key.Builder {

  private final EntityDefinition definition;
  private final Map<Column<?>, Object> columnValues = new HashMap<>();

  private boolean primaryKey = true;

  DefaultKeyBuilder(Entity.Key key) {
    this(key.definition());
    this.primaryKey = key.isPrimaryKey();
    key.columns().forEach(column -> columnValues.put(column, key.get(column)));
  }

  DefaultKeyBuilder(EntityDefinition definition) {
    this.definition = definition;
  }

  @Override
  public <T> Entity.Key.Builder with(Column<T> column, T value) {
    ColumnDefinition<T> columnDefinition = definition.columnDefinition(column);
    if (!columnDefinition.isPrimaryKeyColumn()) {
      primaryKey = false;
    }
    columnValues.put(column, value);

    return this;
  }

  @Override
  public Entity.Key build() {
    return new DefaultKey(definition, initializeValues(new HashMap<>(columnValues)), primaryKey);
  }

  private Map<Column<?>, Object> initializeValues(Map<Column<?>, Object> values) {
    if (primaryKey && !values.isEmpty()) {
      //populate any missing primary key attributes with null values,
      //DefaultKey.equals() relies on the key attributes being present
      definition.primaryKeyColumns().forEach(attribute -> values.putIfAbsent(attribute, null));
    }

    return values;
  }
}
