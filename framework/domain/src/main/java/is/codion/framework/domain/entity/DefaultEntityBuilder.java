/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
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

  DefaultEntityBuilder(Entity.Key key) {
    this(requireNonNull(key).entityDefinition());
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
    AttributeDefinition<T> attributeDefinition = definition.attributes().definition(attribute);
    if (attributeDefinition.isDerived()) {
      throw new IllegalArgumentException("Can not set the value of a derived attribute");
    }
    builderValues.put(attribute, value);

    return this;
  }

  @Override
  public Entity.Builder withDefaultValues() {
    definition.attributes().definitions().stream()
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
