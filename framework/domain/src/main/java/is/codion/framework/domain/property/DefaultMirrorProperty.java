/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

final class DefaultMirrorProperty extends DefaultColumnProperty implements MirrorProperty {

  private static final long serialVersionUID = 1;

  DefaultMirrorProperty(final Attribute<?> propertyId) {
    super(propertyId, -1, null);
    super.setInsertable(false);
    super.setUpdatable(false);
  }
}
