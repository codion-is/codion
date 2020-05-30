/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Entity;

final class DefaultEntityAttribute extends DefaultAttribute<Entity> implements EntityAttribute {

  private static final long serialVersionUID = 1;

  DefaultEntityAttribute(final String name) {
    super(name, Entity.class);
  }
}
