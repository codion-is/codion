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
    return new DefaultKey(definition, initializeValues(new HashMap<>(attributeValues)), primaryKey);
  }

  private Map<Attribute<?>, Object> initializeValues(Map<Attribute<?>, Object> values) {
    if (primaryKey && !values.isEmpty()) {
      //populate any missing primary key attributes with null values,
      //DefaultKey.equals() relies on the key attributes being present
      definition.primaryKeyAttributes().forEach(attribute -> values.putIfAbsent(attribute, null));
    }

    return values;
  }
}
