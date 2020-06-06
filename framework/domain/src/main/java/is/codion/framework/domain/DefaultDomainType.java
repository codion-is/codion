/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import static java.util.Objects.requireNonNull;

class DefaultDomainType implements DomainType {

  private final String name;

  DefaultDomainType(final String name) {
    this.name = requireNonNull(name);
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
    final DefaultDomainType that = (DefaultDomainType) object;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
