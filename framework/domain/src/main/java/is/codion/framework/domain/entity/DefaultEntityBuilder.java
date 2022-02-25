/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.HashMap;
import java.util.Map;

final class DefaultEntityBuilder implements Entity.Builder {

  private final EntityDefinition definition;
  private final Map<Attribute<?>, Object> values;
  private final Map<Attribute<?>, Object> originalValues;
  private final Map<Attribute<?>, Object> builderValues = new HashMap<>();
  private final Map<ForeignKey, Entity> builderForeignKeyValues = new HashMap<>();

  DefaultEntityBuilder(final EntityDefinition definition) {
    this(definition, null, null);
  }

  DefaultEntityBuilder(final EntityDefinition definition, final Map<Attribute<?>, Object> values,
                       final Map<Attribute<?>, Object> originalValues) {
    this.definition = definition;
    this.values = values;
    this.originalValues = originalValues;
  }

  @Override
  public <T> Entity.Builder with(final Attribute<T> attribute, final T value) {
    definition.getProperty(attribute);
    if (attribute instanceof ForeignKey) {
      builderForeignKeyValues.put((ForeignKey) attribute, (Entity) value);
    }
    else {
      builderValues.put(attribute, value);
    }

    return this;
  }

  @Override
  public Entity build() {
    Entity entity = definition.entity(values, originalValues);
    builderValues.forEach((attribute, value) -> entity.put((Attribute<Object>) attribute, value));
    builderForeignKeyValues.forEach(entity::put);

    return entity;
  }
}
