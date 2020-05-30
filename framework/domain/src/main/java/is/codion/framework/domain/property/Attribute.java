/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.io.Serializable;

/**
 * Typed identifier for a {@link Property}.
 * Note that Attributes are equal if their names are equal, the type does not factor into equality.
 * @param <T> the attribute type
 */
public interface Attribute<T> extends Serializable {

  /**
   * @return the name of this attribute.
   */
  String getName();

  /**
   * @return the sql type representing this attribute
   * @see java.sql.Types
   */
  int getType();

  /**
   * @return the Class representing this attribute type
   */
  Class<T> getTypeClass();
}
