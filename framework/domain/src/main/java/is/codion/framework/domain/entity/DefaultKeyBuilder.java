/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.ColumnProperty;

import java.util.HashMap;
import java.util.Map;

final class DefaultKeyBuilder implements Key.Builder {

  private final EntityDefinition definition;
  private final Map<Attribute<?>, Object> attributeValues = new HashMap<>();

  private boolean primaryKey = true;

  DefaultKeyBuilder(Key key) {
    this(key.definition());
    this.primaryKey = key.isPrimaryKey();
    key.attributes().forEach(attribute -> attributeValues.put(attribute, key.get(attribute)));
  }

  DefaultKeyBuilder(EntityDefinition definition) {
    this.definition = definition;
  }

  @Override
  public <T> Key.Builder with(Attribute<T> attribute, T value) {
    ColumnProperty<T> property = definition.columnProperty(attribute);
    if (!property.isPrimaryKeyColumn()) {
      primaryKey = false;
    }
    attributeValues.put(attribute, value);

    return this;
  }

  @Override
  public Key build() {
    if (primaryKey && !attributeValues.isEmpty()) {
      //populate the rest of the primary key attributes with null values
      definition.primaryKeyAttributes().forEach(attribute -> attributeValues.putIfAbsent(attribute, null));
    }

    return new DefaultKey(definition, attributeValues, primaryKey);
  }
}
