/*
 * Copyright (c) 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.function.BinaryOperator;

final class ThrowIfNonUnique implements BinaryOperator<Entity> {

  static final ThrowIfNonUnique INSTANCE = new ThrowIfNonUnique();

  @Override
  public Entity apply(Entity entity, Entity other) {
    throw new IllegalArgumentException("Non-unique primary key: " + entity.primaryKey());
  }
}
