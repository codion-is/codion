/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.Property;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class DefaultEntityBuilder implements Entity.Builder {

  private final EntityDefinition definition;
  private final Map<Attribute<?>, Object> values;
  private final Map<Attribute<?>, Object> originalValues;
  private final Map<Attribute<?>, Object> builderValues = new LinkedHashMap<>();

  DefaultEntityBuilder(Key key) {
    this(requireNonNull(key).definition());
    key.attributes().forEach(attribute -> with((Attribute<Object>) attribute, key.get(attribute)));
  }

  DefaultEntityBuilder(EntityDefinition definition) {
    this(definition, null, null);
  }

  DefaultEntityBuilder(EntityDefinition definition, Map<Attribute<?>, Object> values,
                       Map<Attribute<?>, Object> originalValues) {
    this.definition = definition;
    this.values = values;
    this.originalValues = originalValues;
  }

  @Override
  public <T> Entity.Builder with(Attribute<T> attribute, T value) {
    Property<T> property = definition.property(attribute);
    if (property instanceof DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    builderValues.put(attribute, value);

    return this;
  }

  @Override
  public Entity.Builder withDefaultValues() {
    definition.properties().forEach(property -> {
      if (!(property instanceof DerivedProperty)) {
        builderValues.put(property.attribute(), property.defaultValue());
      }
    });

    return this;
  }

  @Override
  public Entity build() {
    Entity entity = definition.entity(values, originalValues);
    builderValues.forEach((attribute, value) -> entity.put((Attribute<Object>) attribute, value));

    return entity;
  }
}
