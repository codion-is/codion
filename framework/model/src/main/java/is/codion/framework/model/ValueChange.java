/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;

/**
 * Represents a change in a {@link Entity} value.
 * @param <T> the value type
 */
public interface ValueChange<T> {

  /**
   * @return the attribute associated with the changed value
   */
  Attribute<T> getAttribute();

  /**
   * @return the new value
   */
  T getValue();

  /**
   * @return the previous value
   */
  T getPreviousValue();
}
