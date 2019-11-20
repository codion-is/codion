/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
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

  /**
   * Returns an observer notified each time this value changes,
   * may not return null.
   * @return an observer notified each time the value changes
   */
  EventObserver<V> getChangeObserver();

  /**
   * Adds a listener notified each time the value being observed changes
   * @param listener the listener to add
   */
  @Override
  default void addListener(final EventListener listener) {
    getChangeObserver().addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  default void removeListener(final EventListener listener) {
    getChangeObserver().removeListener(listener);
  }

  /**
   * Adds a listener notified with the new value each time the value being observed changes
   * @param listener the listener to add
   */
  @Override
  default void addDataListener(final EventDataListener<V> listener) {
    getChangeObserver().addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  default void removeDataListener(final EventDataListener listener) {
    getChangeObserver().removeDataListener(listener);
  }
}
