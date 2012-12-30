/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * A wrapper class for setting and getting a value
 * @param <V> the type of the value
 */
public interface Value<V> {

  /**
   * Sets the value
   * @param value the value
   */
  void set(final V value);

  /**
   * @return the value
   */
  V get();

  /**
   * @return an event observer notified when the value changes
   */
  EventObserver getChangeEvent();
}
