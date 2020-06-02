/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

final class DefaultEntityAttribute extends DefaultAttribute<Entity> {

  private static final long serialVersionUID = 1;

  DefaultEntityAttribute(final String name, final EntityType entityType) {
    super(name, Entity.class, entityType);
  }

  @Override
  public boolean isEntity() {
    return true;
  }
}
