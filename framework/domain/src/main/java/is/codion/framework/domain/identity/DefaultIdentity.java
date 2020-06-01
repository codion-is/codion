/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.identity;

import static is.codion.common.Util.nullOrEmpty;

class DefaultIdentity implements Identity {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultIdentity(final String name) {
    if (nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    this.name = name;
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    final DefaultIdentity that = (DefaultIdentity) object;

    return name.equals(that.name);
  }

  @Override
  public final int hashCode() {
    return name.hashCode();
  }

  @Override
  public final String toString() {
    return name;
  }
}
