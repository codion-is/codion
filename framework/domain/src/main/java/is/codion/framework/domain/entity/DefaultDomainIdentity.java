/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.identity.DomainIdentity;
import is.codion.framework.domain.identity.Identities;
import is.codion.framework.domain.identity.Identity;

final class DefaultDomainIdentity implements DomainIdentity {

  private static final long serialVersionUID = 1;

  private final Identity identity;

  DefaultDomainIdentity(final String name) {
    this.identity = Identities.identity(name);
  }

  @Override
  public String getName() {
    return identity.getName();
  }

  @Override
  public String toString() {
    return identity.toString();
  }

  @Override
  public int hashCode() {
    return identity.hashCode();
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    final DefaultDomainIdentity that = (DefaultDomainIdentity) object;

    return getName().equals(that.getName());
  }
}
