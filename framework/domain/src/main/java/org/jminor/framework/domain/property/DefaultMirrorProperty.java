/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

final class DefaultMirrorProperty extends DefaultColumnProperty implements MirrorProperty {

  private static final long serialVersionUID = 1;

  DefaultMirrorProperty(final String propertyId) {
    super(propertyId, -1, null);
    super.setReadOnly(true);
  }
}
