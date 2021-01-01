/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;

final class NullColorProvider implements ColorProvider, Serializable {

  private static final long serialVersionUID = 1;

  @Override
  public Object getColor(final Entity entity, final Attribute<?> attribute) {
    return null;
  }
}
