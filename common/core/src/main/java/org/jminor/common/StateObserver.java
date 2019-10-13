/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * Specifies an observer for a {@link State} instance.
 */
public interface StateObserver extends EventObserver<Boolean> {

  /**
   * @return the value of the state being observed
   */
  boolean get();

  /**
   * @return A StateObserver object that is always the reverse of the parent state
   */
  StateObserver getReversedObserver();
}
