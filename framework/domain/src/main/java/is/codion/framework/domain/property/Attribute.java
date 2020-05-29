/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.io.Serializable;

/**
 * Typed identifier for a {@link Property}.
 * Note that Attributes are equal if their names are equal.
 * @param <T> the attribute type
 */
public interface Attribute<T> extends Serializable {

  /**
   * @return the name of this attribute.
   */
  String getName();
}
