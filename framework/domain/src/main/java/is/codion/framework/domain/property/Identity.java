/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.io.Serializable;

/**
 * An identifier for misc. types.
 */
public interface Identity extends Serializable {

  /**
   * @return the identity name, unique within a domain.
   */
  String getName();
}
