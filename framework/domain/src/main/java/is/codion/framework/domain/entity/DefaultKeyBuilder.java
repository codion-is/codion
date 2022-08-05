/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.ColumnProperty;

import java.util.HashMap;
import java.util.Map;

final class DefaultKeyBuilder implements Key.Builder {

  private final EntityDefinition definition;
  private final Map<Attribute<?>, Object> attributeValues = new HashMap<>();

  private boolean primaryKey = false;

  DefaultKeyBuilder(Key key) {
    this(key.getDefinition());
    key.getAttributes().forEach(attribute -> with((Attribute<Object>) attribute, key.get(attribute)));
  }

  DefaultKeyBuilder(EntityDefinition definition) {
    this.definition = definition;
  }

  @Override
  public <T> Key.Builder with(Attribute<T> attribute, T value) {
    ColumnProperty<T> property = definition.getColumnProperty(attribute);
    if (property.primaryKeyColumn()) {
      primaryKey = true;
    }
    attributeValues.put(attribute, value);

    return this;
  }

  @Override
  public Key build() {
    if (primaryKey) {
      definition.getPrimaryKeyAttributes().forEach(attribute -> attributeValues.putIfAbsent(attribute, null));
    }

    return new DefaultKey(definition, attributeValues, primaryKey);
  }
}
