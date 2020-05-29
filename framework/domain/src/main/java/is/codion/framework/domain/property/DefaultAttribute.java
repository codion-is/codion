/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Util;

final class DefaultAttribute<T> implements Attribute<T> {

  private static final long serialVersionUID = 1;

  private final String name;

  DefaultAttribute(final String name) {
    if (Util.nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    this.name = name;
  }

  @Override
  public String getName() {
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
    final DefaultAttribute<?> that = (DefaultAttribute<?>) object;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}
