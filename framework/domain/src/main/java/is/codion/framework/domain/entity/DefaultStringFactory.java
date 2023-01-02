/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A ToString implementation using the entityType plus primary key value.
 */
final class DefaultStringFactory implements Function<Entity, String>, Serializable {

  private static final long serialVersionUID = 1;

  @Override
  public String apply(Entity entity) {
    return entity.type() + ": " + entity.primaryKey();
  }
}
