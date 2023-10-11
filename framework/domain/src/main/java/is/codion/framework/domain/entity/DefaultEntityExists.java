/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.function.Predicate;

final class DefaultEntityExists implements Predicate<Entity>, Serializable {

  private static final long serialVersionUID = 1;

  @Override
  public boolean test(Entity entity) {
    return entity.primaryKey().isNotNull() || entity.originalPrimaryKey().isNotNull();
  }
}
