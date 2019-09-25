/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * A read only value observer
 */
public interface ValueObserver<V> {

  /**
   * @return the value
   */
  V get();

  /**
   * If false then get() is guaranteed to never return null.
   * @return true if this value can be null
   */
  boolean isNullable();

  /**
   * @return an observer notified each time the value changes
   */
  EventObserver<V> getChangeObserver();
}
