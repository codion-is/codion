/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;

import java.io.Serializable;

final class NullColorProvider implements ColorProvider, Serializable {

  private static final long serialVersionUID = 1;

  @Override
  public Object color(Entity entity, Attribute<?> attribute) {
    return null;
  }
}
