/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.util.HashMap;
import java.util.Map;

final class DefaultKeyBuilder implements Entity.Key.Builder {

  private final EntityDefinition definition;
  private final Map<Column<?>, Object> values = new HashMap<>();

  private boolean primaryKey = true;

  DefaultKeyBuilder(Entity.Key key) {
    this(key.entityDefinition());
    this.primaryKey = key.primaryKey();
    key.columns().forEach(column -> values.put(column, key.get(column)));
  }

  DefaultKeyBuilder(EntityDefinition definition) {
    this.definition = definition;
  }

  @Override
  public <T> Entity.Key.Builder with(Column<T> column, T value) {
    ColumnDefinition<T> columnDefinition = definition.columns().definition(column);
    if (!columnDefinition.primaryKey()) {
      primaryKey = false;
    }
    values.put(column, value);

    return this;
  }

  @Override
  public Entity.Key build() {
    return new DefaultKey(definition, initializeValues(new HashMap<>(values)), primaryKey);
  }

  private Map<Column<?>, Object> initializeValues(Map<Column<?>, Object> values) {
    if (primaryKey && !values.isEmpty()) {
      //populate any missing primary key attributes with null values,
      //DefaultKey.equals() relies on the key attributes being present
      definition.primaryKey().columns().forEach(attribute -> values.putIfAbsent(attribute, null));
    }

    return values;
  }
}
