/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

final class DefaultBlobAttribute extends DefaultAttribute<byte[]> {

  private static final long serialVersionUID = 1;

  DefaultBlobAttribute(final String name, final EntityType entityType) {
    super(name, byte[].class, entityType);
  }
}
