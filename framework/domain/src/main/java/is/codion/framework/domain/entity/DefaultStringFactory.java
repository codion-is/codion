/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
            .append(entity.entityDefinition().attributeDefinitions().stream()
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
