/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.identity;

/**
 * A factory for {@link Identity} instances.
 */
public final class Identities {

  private Identities() {}

  /**
   * @param name the identity name
   * @return a identity instance with the given name
   */
  public static Identity identity(final String name) {
    return new DefaultIdentity(name);
  }
}
