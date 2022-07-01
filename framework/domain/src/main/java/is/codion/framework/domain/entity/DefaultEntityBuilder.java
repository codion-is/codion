/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class DefaultEntityBuilder implements Entity.Builder {

  private final EntityDefinition definition;
  private final Map<Attribute<?>, Object> values;
  private final Map<Attribute<?>, Object> originalValues;
  private final Map<Attribute<?>, Object> builderValues = new LinkedHashMap<>();

  DefaultEntityBuilder(Key key) {
    this(requireNonNull(key).getDefinition());
    key.getAttributes().forEach(attribute -> with((Attribute<Object>) attribute, key.get(attribute)));
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
    definition.getProperty(attribute);
    builderValues.put(attribute, value);

    return this;
  }

  @Override
  public Entity.Builder withDefaultValues() {
    definition.getProperties().forEach(property ->
            builderValues.put(property.getAttribute(), property.getDefaultValue()));

    return this;
  }

  @Override
  public Entity build() {
    Entity entity = definition.entity(values, originalValues);
    builderValues.forEach((attribute, value) -> entity.put((Attribute<Object>) attribute, value));

    return entity;
  }
}
