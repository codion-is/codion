/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

/**
 * A wrapper class for setting and getting a value
 * @param <V> the type of the value
 */
public interface Value<V> extends ValueObserver<V> {

  /**
   * Sets the value, setting the same value again does not trigger a value change
   * @param value the value
   */
  void set(V value);

  /**
   * Links the given value to this value, so that changes in one are reflected in the other
   * @param linkedValue the linked value
   * @param <V> the value type
   */
  void link(Value<V> linkedValue);

  /**
   * Links the given value to this value, so that changes in this value are reflected in the linked value
   * @param linkedValue the linked value
   * @param oneWay if true this value is not updated if the linked value changes
   * @param <V> the value type
   */
  void link(Value<V> linkedValue, boolean oneWay);
}
