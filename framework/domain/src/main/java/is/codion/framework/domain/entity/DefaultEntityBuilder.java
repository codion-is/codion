/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;

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
    key.columns().forEach(attribute -> with((Attribute<Object>) attribute, key.get(attribute)));
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
    AttributeDefinition<T> definition = this.definition.attributeDefinition(attribute);
    if (definition.isDerived()) {
      throw new IllegalArgumentException("Can not set the value of a derived attribute");
    }
    builderValues.put(attribute, value);

    return this;
  }

  @Override
  public Entity.Builder withDefaultValues() {
    definition.attributeDefinitions().stream()
            .filter(AttributeDefinition::hasDefaultValue)
            .forEach(attributeDefinition -> builderValues.put(attributeDefinition.attribute(), attributeDefinition.defaultValue()));

    return this;
  }

  @Override
  public Entity build() {
    Entity entity = definition.entity(values, originalValues);
    builderValues.forEach((attribute, value) -> entity.put((Attribute<Object>) attribute, value));

    return entity;
  }
}
