/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.event.EventObserver;

/**
 * A read only value observer
 * @param <V> the type of the value
 */
public interface ValueObserver<V> extends EventObserver<V> {

  /**
   * @return the value
   */
  V get();

  /**
   * If false then get() is guaranteed to never return null.
   * @return true if this value can be null
   */
  boolean isNullable();
}
