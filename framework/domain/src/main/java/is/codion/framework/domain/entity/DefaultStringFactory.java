/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.Property;

import java.io.Serializable;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * A ToString implementation using the entityType plus primary key value.
 */
final class DefaultStringFactory implements Function<Entity, String>, Serializable {

  private static final long serialVersionUID = 1;

  @Override
  public String apply(Entity entity) {
    return new StringBuilder(entity.type().name())
            .append(entity.definition().properties().stream()
                    .map(Property::attribute)
                    .map(attribute -> attribute.name() + ": " + entity.toString(attribute))
                    .collect(joining(", ", ": ", "")))
            .toString();
  }
}
