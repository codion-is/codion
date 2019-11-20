/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

/**
 * A Value bound to a property
 * @param <T> the value type
 */
public interface PropertyValue<T> extends Value<T> {

  /**
   * @return the name of the property this value is associated with
   */
  String getProperty();
}
