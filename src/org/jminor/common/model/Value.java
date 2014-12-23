/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * A wrapper class for setting and getting a value
 * @param <V> the type of the value
 */
public interface Value<V> {

  /**
   * Sets the value, setting the same value again does not trigger a value change
   * @param value the value
   */
  void set(final V value);

  /**
   * @return the value
   */
  V get();

  /**
   * @return an observer notified with the new value each time it changes
   */
  EventObserver<V> getChangeObserver();
}
