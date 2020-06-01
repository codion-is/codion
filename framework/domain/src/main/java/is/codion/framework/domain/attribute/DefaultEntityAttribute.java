/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.identity.Identity;

final class DefaultEntityAttribute extends DefaultAttribute<Entity> implements EntityAttribute {

  private static final long serialVersionUID = 1;

  DefaultEntityAttribute(final String name, final Identity entityId) {
    super(name, Entity.class, entityId);
  }
}
