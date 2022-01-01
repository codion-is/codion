/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A ToString implementation using the entityType plus primary key value.
 */
final class DefaultStringProvider implements Function<Entity, String>, Serializable {

  private static final long serialVersionUID = 1;

  @Override
  public String apply(final Entity entity) {
    return entity.getEntityType() + ": " + entity.getPrimaryKey();
  }
}
