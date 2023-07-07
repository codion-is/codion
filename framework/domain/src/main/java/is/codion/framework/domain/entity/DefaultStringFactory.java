/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

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
            .append(entity.entrySet().stream()
                    .map(entry -> entry.getKey().name() + ":" + entity.toString(entry.getKey()))
                    .collect(joining(", ", ": ", "")))
            .toString();
  }
}
