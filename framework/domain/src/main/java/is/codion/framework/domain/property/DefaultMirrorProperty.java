/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

final class DefaultMirrorProperty<T> extends DefaultColumnProperty<T> implements MirrorProperty<T> {

  private static final long serialVersionUID = 1;

  DefaultMirrorProperty(final Attribute<T> attribute) {
    super(attribute, null);
    super.setInsertable(false);
    super.setUpdatable(false);
  }
}
