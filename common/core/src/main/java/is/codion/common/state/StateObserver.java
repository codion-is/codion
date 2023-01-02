/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.value.ValueObserver;

/**
 * Specifies an observer for a {@link State} instance.
 */
public interface StateObserver extends ValueObserver<Boolean> {

  /**
   * @return A {@link StateObserver} instance that is always the reverse of this {@link StateObserver} instance
   */
  StateObserver reversedObserver();
}
