/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import static java.util.Objects.requireNonNull;

public final class DomainType<T extends Domain> {

  private final String name;

  public DomainType(final Class<T> domainClass) {
    this(requireNonNull(domainClass).getSimpleName());
  }

  public DomainType(final String name) {
    requireNonNull(name);
    this.name = name;
  }

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
    final DomainType<?> that = (DomainType<?>) object;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
