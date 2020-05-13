/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.framework.domain.property.Property;

import java.io.Serializable;

final class NullColorProvider implements ColorProvider, Serializable {

  private static final long serialVersionUID = 1;

  @Override
  public Object getColor(final Entity entity, final Property property) {
    return null;
  }
}
