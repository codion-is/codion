/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * Specifies an observer for a {@link State} instance.
 */
public interface StateObserver extends EventObserver<Boolean> {

  /**
   * @return true if the state being observed is active, false otherwise
   */
  boolean isActive();

  /**
   * @return an EventObserver notified each time the observed state changes
   */
  EventObserver<Boolean> getChangeObserver();

  /**
   * @return A StateObserver object that is always the reverse of the parent state
   */
  StateObserver getReversedObserver();
}
