/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.HashMap;
import java.util.Map;

final class DefaultEntityBuilder implements Entity.Builder {

  private final EntityDefinition definition;
  private final Map<Attribute<?>, Object> attributeValues = new HashMap<>();
  private final Map<ForeignKey, Entity> foreignKeyValues = new HashMap<>();

  DefaultEntityBuilder(final EntityDefinition definition) {
    this.definition = definition;
  }

  @Override
  public <T> Entity.Builder with(final Attribute<T> attribute, final T value) {
    definition.getProperty(attribute);
    if (attribute instanceof ForeignKey) {
      foreignKeyValues.put((ForeignKey) attribute, (Entity) value);
    }
    else {
      attributeValues.put(attribute, value);
    }

    return this;
  }

  @Override
  public Entity build() {
    final Entity entity = definition.entity(attributeValues, null);
    foreignKeyValues.forEach(entity::put);

    return entity;
  }
}
