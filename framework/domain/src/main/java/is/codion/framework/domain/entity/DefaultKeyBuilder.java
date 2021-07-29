/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.ColumnProperty;

import java.util.HashMap;
import java.util.Map;

final class DefaultKeyBuilder implements Key.Builder {

  private final EntityDefinition definition;
  private final Map<Attribute<?>, Object> attributeValues = new HashMap<>();

  private boolean primaryKey = false;

  DefaultKeyBuilder(final Key key, final EntityDefinition definition) {
    this(definition);
    key.getAttributes().forEach(attribute -> with((Attribute<Object>) attribute, key.get(attribute)));
  }

  DefaultKeyBuilder(final EntityDefinition definition) {
    this.definition = definition;
  }

  @Override
  public <T> Key.Builder with(final Attribute<T> attribute, final T value) {
    final ColumnProperty<T> property = definition.getColumnProperty(attribute);
    if (property.isPrimaryKeyColumn()) {
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
