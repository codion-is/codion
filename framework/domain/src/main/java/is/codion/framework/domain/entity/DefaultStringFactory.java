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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;

import java.io.Serializable;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * A ToString implementation using the entityType plus available attribute values.
 */
final class DefaultStringFactory implements Function<Entity, String>, Serializable {

  private static final long serialVersionUID = 1;

  @Override
  public String apply(Entity entity) {
    return new StringBuilder(entity.entityType().name())
            .append(entity.definition().attributes().definitions().stream()
                    .map(AttributeDefinition::attribute)
                    .filter(entity::contains)
                    .map(attribute -> toString(entity, attribute))
                    .collect(joining(", ", ": ", "")))
            .toString();
  }

  private static String toString(Entity entity, Attribute<?> attribute) {
    return attribute.name() + ": " + (entity.isNull(attribute) ? "null" : entity.toString(attribute));
  }
}
