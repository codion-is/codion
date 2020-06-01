/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

final class DefaultDomainIdentity extends DefaultIdentity implements DomainIdentity {

  private static final long serialVersionUID = 1;

  DefaultDomainIdentity(final String name) {
    super(name);
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
