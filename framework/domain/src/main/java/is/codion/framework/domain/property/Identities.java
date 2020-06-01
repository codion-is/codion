/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.EntityIdentity;

public final class Identities {

  private Identities() {}

  public static DomainIdentity domainIdentity(final String name) {
    return new DefaultDomainIdentity(name);
  }

  /**
   * @param name the identity name
   * @return a {@link EntityIdentity} instance with the given name
   */
  public static EntityIdentity entityIdentity(final String name) {
    return new DefaultEntityIdentity(name);
  }
}
