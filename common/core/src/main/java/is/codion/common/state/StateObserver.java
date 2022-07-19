/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.value.ValueObserver;

/**
 * Specifies an observer for a {@link State} instance.
 */
public interface StateObserver extends ValueObserver<Boolean> {

  /**
   * @return A StateObserver instance that is always the reverse of this StateObserver
   */
  StateObserver getReversedObserver();
}
