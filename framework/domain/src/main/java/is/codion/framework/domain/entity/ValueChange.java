/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.attribute.Attribute;

/**
 * Represents a change in a {@link Entity} value.
 */
public interface ValueChange {

  /**
   * @return the attribute associated with the changed value
   */
  Attribute<?> getAttribute();

  /**
   * @return the new value
   */
  Object getValue();

  /**
   * @return the previous value
   */
  Object getPreviousValue();

  /**
   * @return true if the property had no associated value prior to this value change
   */
  boolean isInitialization();
}
