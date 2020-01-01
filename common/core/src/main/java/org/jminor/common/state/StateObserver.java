/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.state;

import org.jminor.common.event.EventObserver;

/**
 * Specifies an observer for a {@link State} instance.
 */
public interface StateObserver extends EventObserver<Boolean> {

  /**
   * @return the value of the state being observed
   */
  boolean get();

  /**
   * @return A StateObserver instance that is always the reverse of this StateObserver
   */
  StateObserver getReversedObserver();
}
