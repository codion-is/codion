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
   * @return an observer notified each time the value changes
   */
  EventObserver<V> getChangeObserver();
}
