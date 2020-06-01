/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;

/**
 * An identifier for entity types.
 */
public interface Identity extends Serializable {

  /**
   * @return the identity name, unique within a domain.
   */
  String getName();

  /**
   * @param name the identity name
   * @return a Identity instance with the given name
   */
  static Identity identity(final String name) {
    return new DefaultIdentity(name);
  }
}
