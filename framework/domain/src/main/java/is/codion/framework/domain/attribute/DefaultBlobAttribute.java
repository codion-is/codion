/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.attribute;

import is.codion.framework.domain.entity.EntityIdentity;

final class DefaultBlobAttribute extends DefaultAttribute<byte[]> {

  private static final long serialVersionUID = 1;

  DefaultBlobAttribute(final String name, final EntityIdentity entityId) {
    super(name, byte[].class, entityId);
  }
}
