/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityIdentity;

final class DefaultEntityAttribute extends DefaultAttribute<Entity> {

  private static final long serialVersionUID = 1;

  DefaultEntityAttribute(final String name, final EntityIdentity entityId) {
    super(name, Entity.class, entityId);
  }

  @Override
  public boolean isEntity() {
    return true;
  }
}
