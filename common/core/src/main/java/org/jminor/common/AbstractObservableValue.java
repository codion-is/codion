/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * A base Value implementation handling the {@link ValueObserver} for a Value.
 * @param <V> the value type
 */
public abstract class AbstractObservableValue<V> implements Value<V> {

  private ValueObserver<V> observer;

  /** {@inheritDoc} */
  @Override
  public final ValueObserver<V> getObserver() {
    if (observer == null) {
      observer = Values.valueObserver(this);
    }

    return observer;
  }
}
