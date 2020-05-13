/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.framework.domain.property.Property;

/**
 * Represents a change in a {@link Entity} value.
 */
public interface ValueChange {

  /**
   * @return the Property associated with the changed value
   */
  Property getProperty();

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
